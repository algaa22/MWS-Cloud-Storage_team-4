package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ChangeMetadataRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.UploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileLockedException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Обертка-фасад для управления жизненным циклом операций над файлами и директориями.
 *
 * <p>Класс реализует логику "умного" выполнения операций, обеспечивая:
 *
 * <ul>
 *   <li><b>Управление состояниями (FSM):</b> перевод сущностей между READY, PENDING, ERROR и FATAL.
 *   <li><b>Отказоустойчивость:</b> использование библиотеки Failsafe для автоматических повторов
 *       (retries) при возникновении восстановимых ошибок. Когда Failsafe исчерпывает количество
 *       попыток, при некоторых операциях происходит {@code client-side} retry.
 *   <li><b>Синхронизацию метаданных:</b> гарантированное обновление состояния в БД после выполнения
 *       действий во внешнем хранилище (S3).
 *   <li><b>Предотвращение конфликтов:</b> проверку статуса перед началом работы (Optimistic Locking
 *       на уровне бизнес-логики).
 * </ul>
 *
 * <p>Механизм ретраев опирается на {@link dev.failsafe.RetryPolicy}. При исчерпании лимита попыток
 * или возникновении критической ошибки сущность переводится в терминальный статус FATAL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageRepositoryWrapper {
  private final StorageJpaRepositoryAdapter metadataRepository;
  private final TransactionTemplate transactionTemplate;
  private final StorageConfig storageConfig;
  private final RetryPolicy<Object> retryPolicy;
  private final EntityManager entityManager;

  /**
   * Выполняет обновление существующего файла.
   *
   * <p>Для вызова требуется статус READY. <br>
   * Сначала переводит файл в статус {@code PENDING} в базе, затем выполняет операцию. <br>
   * После успеха автоматически ставит {@code READY} и сохраняет все изменения в БД.
   */
  public <T> void wrapUpdate(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsReady(entity);
    prepareEntityToPending(entity, operationType);
    finalizeOperation(entity, operationType, operation);
  }

  /**
   * Выполняет создание нового файла.
   *
   * <p>После выполнения операции переводит статус в {@code READY} и сохраняет все изменения в БД
   */
  public <T> void wrapNewEntityTask(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    finalizeOperation(entity, operationType, operation);
  }

  /**
   * Начинает сложную операцию.
   *
   * <p>Для вызова требуется статус READY. <br>
   * Перед выполнением операции сам создает файл и переводит статус в {@code PENDING}
   */
  public <T> T initiateStep(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsReady(entity);
    prepareEntityToPending(entity, operationType);

    return executeOperation(entity, operationType, operation);
  }

  /**
   * Работает с файлом, который уже находится в обработке ({@code PENDING}).
   *
   * <p>Для вызова требуется статус PENDING. <br>
   * Статус файла в конце НЕ меняет. Подходит для промежуточных этапов вроде загрузки чанков.
   */
  public <T> T processStep(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsPending(entity);

    T result = executeOperation(entity, operationType, operation);

    if (shouldThrottledUpdate(entity)) {
      syncEntityWithDatabase(entity, operationType, FileStatus.PENDING);
    }

    return result;
  }

  /**
   * Завершает сложную операцию.
   *
   * <p>Для вызова требуется статус PENDING. <br>
   * Блокирует строку в БД, выполняет операцию и переводит файл в {@code READY}.<br>
   * Автоматически сохраняет любые изменения {@code entity}, сделанные в лямбде.
   */
  public <T> void completeStep(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsPending(entity);
    finalizeOperation(entity, operationType, operation);
  }

  /** Принудительно помечает операцию как {@code READY} и устанавливает {@code retry_count = 0}. */
  public void resetToReady(StorageEntity entity) {
    syncEntityWithDatabase(entity, FileOperationType.CHANGE_METADATA, FileStatus.READY, 0);
  }

  private <T> T executeOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    try {
      return Failsafe.with(retryPolicy).get(operation::apply);
    } catch (BaseStorageException exception) {
      handleException(exception, entity, operationType);

      throw exception;
    } catch (Exception exception) {
      FatalStorageException fatalException = new FatalStorageException(exception);
      handleException(fatalException, entity, operationType);

      throw fatalException;
    }
  }

  private <T> void finalizeOperation(
          StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {

    executeOperation(entity, operationType, operation);

    syncEntityWithDatabase(entity, operationType, FileStatus.READY);
  }

  private void checkIfStatusIsReady(StorageEntity entity) {
    checkIfStatusIs(entity, FileStatus.READY);
  }

  private void checkIfStatusIsPending(StorageEntity entity) {
    checkIfStatusIs(entity, FileStatus.PENDING);
  }

  private void checkIfStatusIs(StorageEntity entity, FileStatus expectedStatus) {
    if (entity.getStatus() != expectedStatus) {
      throw new StorageFileLockedException(entity.getParentId(), entity.getName());
    }
  }

  private void prepareEntityToPending(StorageEntity entity, FileOperationType operationType) {
    entity.setOperationType(operationType);
    entity.setStartedAt(LocalDateTime.now());

    syncEntityWithDatabase(entity, operationType, FileStatus.PENDING, 0);
  }

  private boolean shouldThrottledUpdate(StorageEntity entity) {
    int throttledUpdateInterval = storageConfig.stateMachine().fileThrottledUpdateIntervalSec();

    return entity.getUpdatedAt() == null
        || entity
            .getUpdatedAt()
            .isBefore(LocalDateTime.now().minusSeconds(throttledUpdateInterval));
  }

  private void handleException(
      Throwable throwable, StorageEntity entity, FileOperationType operationType) {
    entity.setErrorMessage(throwable.getMessage());

    if (throwable instanceof RecoverableStorageException exception) {
      handleRecoverableException(exception, entity, operationType);
    } else if (throwable instanceof FatalStorageException exception) {
      handleFatalException(exception, entity, operationType);
    } else {
      handleBusinessException(entity, operationType);
    }
  }

  private void handleRecoverableException(
      RecoverableStorageException exception,
      StorageEntity entity,
      FileOperationType operationType) {
    if (entity.getRetryCount() >= storageConfig.stateMachine().maxRetryCount()) {
      log.error(
          "FATAL: Max retry count reached for operation {}, userId: {}, fileId: {}",
          operationType,
          entity.getUserId(),
          entity.getId(),
          exception);
      syncEntityWithDatabase(entity, operationType, FileStatus.FATAL);
      return;
    }

    if (entity.getRetryCount() == 0) {
      initiateRetryStrategy(exception, entity, operationType);
    }

    syncEntityWithDatabase(entity, operationType, FileStatus.ERROR, entity.getRetryCount() + 1);
  }

  private void handleFatalException(
      FatalStorageException exception, StorageEntity entity, FileOperationType operationType) {
    log.error(
        "FATAL: Failed to perform operation {}, userId: {}, fileId: {}",
        operationType,
        entity.getUserId(),
        entity.getId(),
        exception);
    syncEntityWithDatabase(entity, operationType, FileStatus.FATAL);
  }

  private void handleBusinessException(StorageEntity entity, FileOperationType operationType) {
    syncEntityWithDatabase(entity, operationType, FileStatus.READY);
  }

  /**
   * Реализует стратегию "ленивого восстановления" при возникновении мягких ошибок. *
   *
   * <p>Вместо немедленного удаления метаданных при сбое (что могло бы нарушить логику
   * возобновляемых загрузок), метод просто делегирует управление исключениями вызывающей стороне. *
   *
   * <ul>
   *   <li><b>UPLOAD:</b> Оставляет сущность в базе "как есть" (обычно в статусе PENDING или ERROR).
   *       Это критически важно для Resumable Upload, позволяя клиенту продолжить загрузку. Очистка
   *       реально заброшенных сессий ложится на {@code StaleFileCleanupService}. *
   *   <li><b>CHANGE_METADATA:</b> Сбрасывает статус в READY и обнуляет счетчик ретраев. Это
   *       разблокирует файл для последующих попыток редактирования пользователем.
   * </ul>
   *
   * * @param exception Исходная ошибка хранилища.
   *
   * @param entity Сущность, на которой произошел сбой.
   * @param operationType Тип операции, определяющий логику уведомления.
   */
  private void initiateRetryStrategy(
      RecoverableStorageException exception,
      StorageEntity entity,
      FileOperationType operationType) {
    switch (operationType) {
      case UPLOAD -> throw new UploadRetriableException(exception);
      case CHANGE_METADATA -> {
        syncEntityWithDatabase(entity, operationType, FileStatus.READY, 0);
        throw new ChangeMetadataRetriableException(exception);
      }
    }
  }

  private void syncEntityWithDatabase(
      StorageEntity entity, FileOperationType operationType, FileStatus newStatus) {
    syncEntityWithDatabase(entity, operationType, newStatus, entity.getRetryCount());
  }

  private void syncEntityWithDatabase(
      StorageEntity entity,
      FileOperationType operationType,
      FileStatus newStatus,
      int newRetryCount) {
    transactionTemplate.execute(
        status -> {
          if (operationType == FileOperationType.DELETE) {
            return null;
          }

          StorageEntity managedEntity = syncToPersistenceContext(entity);

          managedEntity.setStatus(newStatus);
          managedEntity.setRetryCount(newRetryCount);

          if (newStatus == FileStatus.PENDING || newStatus == FileStatus.READY) {
            managedEntity.setUpdatedAt(LocalDateTime.now());
          }

          if (newStatus == FileStatus.READY) {
            managedEntity.setOperationType(null);
          }

          metadataRepository.updateFile(managedEntity);

          return null;
        });
  }

  private StorageEntity syncToPersistenceContext(StorageEntity entity) {
    System.out.println("=== syncToPersistenceContext DEEP DEBUG ===");
    System.out.println("Entity ID: " + entity.getId());
    System.out.println("Entity hash: " + System.identityHashCode(entity));

    // 1. Пробуем найти через JPA
    StorageEntity managed = entityManager.find(StorageEntity.class, entity.getId());
    System.out.println("JPA find result: " + (managed != null ? "found" : "null"));

    if (managed != null) {
      System.out.println("JPA found entity with hash: " + System.identityHashCode(managed));
      return entityManager.merge(entity);
    }

    // 2. Если JPA не нашел, пробуем прямой SQL
    String sql = "SELECT * FROM files WHERE id = :id";
    Query query = entityManager.createNativeQuery(sql, StorageEntity.class);
    query.setParameter("id", entity.getId());

    List<StorageEntity> result = query.getResultList();
    System.out.println("SQL query result size: " + result.size());

    if (!result.isEmpty()) {
      StorageEntity fromDb = result.get(0);
      System.out.println("Found in DB via SQL! ID: " + fromDb.getId());
      System.out.println("Is deleted in DB: " + fromDb.isDeleted());

      // УБИРАЕМ REFRESH - ОН НЕ НУЖЕН!

      // Просто делаем merge
      StorageEntity merged = entityManager.merge(fromDb);
      System.out.println("After merge - hash: " + System.identityHashCode(merged));
      return merged;
    }

    // 3. Если действительно нет в БД
    System.out.println("Entity not found in DB at all - doing persist");
    entityManager.persist(entity);
    return entity;
  }

  @FunctionalInterface
  public interface FileOperation<T> {
    T apply() throws BaseStorageException;
  }
}

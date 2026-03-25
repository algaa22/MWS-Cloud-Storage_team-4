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
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
  private final StorageConfig storageConfig;
  private final RetryPolicy<Object> retryPolicy;

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
    syncEntityWithDatabase(entity, operationType, FileStatus.PENDING, 0);
    finalizeOperation(entity, operationType, operation);
  }

  /**
   * Выполняет создание нового файла.
   *
   * <p>После выполнения операции переводит статус в {@code READY} и сохраняет все изменения в БД
   */
  public <T> void wrapNewEntityTask(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    markAsPending(entity, operationType);
    finalizeOperation(entity, operationType, operation);
  }

  /**
   * Начинает сложную операцию.
   *
   * <p>Просто вызывает переданный метод
   */
  public <T> void initiateNewFileStep(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    markAsPending(entity, operationType);
    executeOperation(entity, operationType, operation);
  }

  /**
   * Работает с файлом, который уже находится в обработке ({@code PENDING}).
   *
   * <p>Для вызова требуется статус PENDING. <br>
   * Статус файла в конце НЕ меняет. Подходит для промежуточных этапов вроде загрузки чанков.
   */
  public <T> void processStep(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsPending(entity);
    executeOperation(entity, operationType, operation);

    if (shouldThrottledUpdate(entity)) {
      syncEntityWithDatabase(entity, operationType, FileStatus.PENDING);
    }
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

  private <T> void executeOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    try {
      Failsafe.with(retryPolicy).get(operation::apply);
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

  private void markAsPending(StorageEntity entity, FileOperationType operationType) {
    entity.setStatus(FileStatus.PENDING);
    entity.setOperationType(operationType);
    entity.setStartedAt(LocalDateTime.now());
    entity.setRetryCount(0);
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
   * Реализует стратегию "ленивого восстановления" при возникновении мягких ошибок.
   *
   * <p>Вместо немедленного удаления метаданных при сбое (что могло бы нарушить логику
   * возобновляемых загрузок), метод просто делегирует управление исключениями вызывающей стороне.
   *
   * <ul>
   *   <li><b>UPLOAD:</b> Оставляет сущность в базе "как есть" (обычно в статусе PENDING или ERROR).
   *       Это критически важно для Resumable Upload, позволяя клиенту продолжить загрузку. Очистка
   *       реально заброшенных сессий ложится на {@code StaleFileCleanupService}.
   *   <li><b>CHANGE_METADATA:</b> Сбрасывает статус в READY и обнуляет счетчик ретраев. Это
   *       разблокирует файл для последующих попыток редактирования пользователем.
   * </ul>
   *
   * @param exception Исходная ошибка хранилища.
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
    if (operationType == FileOperationType.DELETE) {
      return;
    }

    metadataRepository.syncLifecycleMetadata(
        entity.getId(),
        newStatus,
        newRetryCount,
        (newStatus == FileStatus.READY ? null : operationType));
  }

  @FunctionalInterface
  public interface FileOperation<T> {
    T apply() throws BaseStorageException;
  }
}

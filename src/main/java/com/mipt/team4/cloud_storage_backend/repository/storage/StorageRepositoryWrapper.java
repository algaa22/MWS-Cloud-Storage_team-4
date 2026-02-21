package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.storage.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.storage.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileLockedException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageRepositoryWrapper {
  private final FileMetadataRepository metadataRepository;
  private final StorageConfig storageConfig;

  // TODO: @Transactional

  /**
   * Выполняет обновление существующего файла.
   *
   * <p>Для вызова требуется статус READY. <br>
   * Сначала переводит файл в статус {@code PENDING} в базе, затем выполняет операцию. <br>
   * После успеха автоматически ставит {@code READY} и сохраняет все изменения в БД.
   */
  public <T> void executeUpdateOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsReady(entity);
    prepareEntity(entity, operationType);
    finalizeOperation(entity, operationType, operation);
  }

  /**
   * Выполняет создание нового файла.
   *
   * <p>После выполнения операции переводит статус в {@code READY} и сохраняет все изменения в БД
   */
  public <T> void executeCreateOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    finalizeOperation(entity, operationType, operation);
  }

  /**
   * Начинает сложную операцию.
   *
   * <p>Для вызова требуется статус READY. <br>
   * После выполнения операции переводит статус в {@code PENDING}
   */
  public <T> T executeStartComplexOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsReady(entity);
    prepareEntity(entity, operationType);

    return executeOperation(entity, operationType, operation);
  }

  /**
   * Работает с файлом, который уже находится в обработке ({@code PENDING}).
   *
   * <p>Для вызова требуется статус PENDING. <br>
   * Блокирует строку в БД через {@code SELECT FOR UPDATE}.<br>
   * Статус файла в конце НЕ меняет. Подходит для промежуточных этапов вроде загрузки чанков.
   */
  public <T> T executeInProgressOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsPending(entity);

    T result = executeOperation(entity, operationType, operation);

    if (shouldThrottledUpdate(entity)) {
      finalizeEntityUpdate(entity, FileStatus.PENDING);
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
  public <T> void executeFinalComplexOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsPending(entity);

    finalizeOperation(entity, operationType, operation);
  }

  /** Принудительно помечает операцию как {@code READY} и устанавливает {@code retry_count = 0}. */
  public void forceRollbackOperation(StorageEntity entity) {
    finalizeEntityUpdate(entity, FileStatus.READY, 0);
  }

  private <T> T executeOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    try {
      return operation.apply();
    } catch (Exception exception) {
      handleException(exception, entity, operationType);
      throw new RuntimeException(exception);
    }
  }

  private <T> void finalizeOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    executeOperation(entity, operationType, operation);
    finalizeEntityUpdate(entity, FileStatus.READY);
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

  private void prepareEntity(StorageEntity entity, FileOperationType operationType) {
    entity.setOperationType(operationType);
    entity.setStartedAt(LocalDateTime.now());

    finalizeEntityUpdate(entity, FileStatus.PENDING, 0);
  }

  private void finalizeEntityUpdate(StorageEntity entity, FileStatus newStatus) {
    finalizeEntityUpdate(entity, newStatus, entity.getRetryCount());
  }

  private boolean shouldThrottledUpdate(StorageEntity entity) {
    int throttledUpdateInterval = storageConfig.stateMachine().fileThrottledUpdateIntervalSec();

    return entity.getUpdatedAt() == null
        || entity
            .getUpdatedAt()
            .isBefore(LocalDateTime.now().minusSeconds(throttledUpdateInterval));
  }

  private void handleException(
      Exception exception, StorageEntity entity, FileOperationType operationType) {
    entity.setErrorMessage(exception.getMessage());

    if (exception instanceof RecoverableStorageException) {
      finalizeEntityUpdate(entity, FileStatus.ERROR, entity.getRetryCount() + 1);
    } else if (exception instanceof FatalStorageException) {
      log.error("FATAL: Failed to perform operation {}", operationType, exception);
      finalizeEntityUpdate(entity, FileStatus.FATAL);
    } else {
      finalizeEntityUpdate(entity, FileStatus.READY);
    }
  }

  private void finalizeEntityUpdate(StorageEntity entity, FileStatus newStatus, int newRetryCount) {
    entity.setStatus(newStatus);
    entity.setRetryCount(newRetryCount);

    if (newStatus == FileStatus.PENDING || newStatus == FileStatus.READY) {
      entity.setUpdatedAt(LocalDateTime.now());
    }

    if (newStatus == FileStatus.READY) {
      entity.setOperationType(null);
    }

    try {
      metadataRepository.updateEntity(entity);
    } catch (Exception e) {
      log.error("FATAL: Failed to update file entity {}", entity.getId(), e);
    }
  }

  @FunctionalInterface
  public interface FileOperation<T> {
    T apply() throws Exception;
  }
}

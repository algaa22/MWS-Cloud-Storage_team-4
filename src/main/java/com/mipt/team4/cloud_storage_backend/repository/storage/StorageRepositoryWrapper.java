package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.storage.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageRepositoryWrapper {
  private final FileMetadataRepository metadataRepository;

  /**
   * Выполняет обновление существующего файла.
   * <p>
   * Сначала переводит файл в статус {@code PENDING} в базе, затем выполняет операцию. <br>
   * После успеха автоматически ставит {@code READY} и сохраняет все изменения в БД.
   */
  public <T> void executeUpdateOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    prepareEntityForUpdate(entity, operationType);
    finalizeOperation(entity, operationType, operation);
  }

  /**
   * Выполняет создание нового файла.
   * <p>
   * После выполнения операции переводит статус в {@code READY}<br>
   * После успеха автоматически ставит {@code READY} и сохраняет все изменения в БД.
   */
  public <T> void executeCreateOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    finalizeOperation(entity, operationType, operation);
  }

  /**
   * Работает с файлом, который уже находится в обработке ({@code PENDING}).
   * <p>
   * Блокирует строку в БД через {@code SELECT FOR UPDATE}.<br>
   * Статус файла в конце НЕ меняет. Подходит для промежуточных этапов вроде загрузки чанков.
   */
  public <T> T executeInProgressOperation(
      UUID fileId, FileOperationType operationType, FileOperation<T> operation) {
    StorageEntity entity = getEntityByFileId(fileId);
    checkIfStatusIsPending(entity);

    return executeOperation(entity, operationType, operation);
  }

  /**
   * Финализирует длительную операцию.
   * <p>
   * Блокирует строку в БД, выполняет операцию и переводит файл в {@code READY}.<br>
   * Автоматически сохраняет любые изменения {@code entity}, сделанные в лямбде.
   */
  public <T> void executeFinalOperation(
      UUID fileId, FileOperationType operationType, FileOperation<T> operation) {
    StorageEntity entity = getEntityByFileId(fileId);
    checkIfStatusIsPending(entity);
    finalizeOperation(entity, operationType, operation);
  }

  public <T> T executeOperation(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    try {
      return operation.apply(entity);
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

  // TODO: @Transactional
  private StorageEntity getEntityByFileId(UUID fileId) {
    return metadataRepository
        .getFileForUpdate(fileId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "FATAL: StorageEntity not found for id "
                        + fileId
                        + ". This implies a race condition or other critical error"));
  }

  private void checkIfStatusIsPending(StorageEntity entity) {
    if (entity.getStatus() != FileStatus.PENDING) {
      throw new IllegalStateException(
          "FATAL: Cannot perform InProgress operation on file "
              + entity.getId()
              + " in status "
              + entity.getStatus());
    }
  }

  private void prepareEntityForUpdate(StorageEntity entity, FileOperationType operationType) {
    entity.setOperationType(operationType);
    entity.setStartedAt(LocalDateTime.now());

    finalizeEntityUpdate(entity, FileStatus.PENDING, 0);
  }

  private void finalizeEntityUpdate(StorageEntity entity, FileStatus newStatus) {
    finalizeEntityUpdate(entity, newStatus, entity.getRetryCount());
  }

  private void finalizeEntityUpdate(StorageEntity entity, FileStatus newStatus, int newRetryCount) {
    entity.setStatus(newStatus);
    entity.setRetryCount(newRetryCount);

    if (newStatus == FileStatus.READY) {
      entity.setUpdatedAt(LocalDateTime.now());
    }

    try {
      metadataRepository.updateEntity(entity);
    } catch (Exception e) {
      log.error("FATAL: Failed to update file entity {}", entity.getId());
    }
  }

  private void handleException(
      Exception exception, StorageEntity entity, FileOperationType operationType) {
    if (exception instanceof RecoverableStorageException) {
      finalizeEntityUpdate(entity, FileStatus.ERROR, entity.getRetryCount() + 1);
    } else if (exception instanceof FatalStorageException) {
      log.error("FATAL: Failed to perform operation {}", operationType);
      finalizeEntityUpdate(entity, FileStatus.FATAL);
    } else {
      finalizeEntityUpdate(entity, FileStatus.READY);
    }
  }

  @FunctionalInterface
  public interface FileOperation<T> {
    T apply(StorageEntity entity) throws Exception;
  }
}

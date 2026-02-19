package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.storage.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageRepositoryWrapper {
  private final FileMetadataRepository metadataRepository;

  public <T> T executeUpdateOperation(
      UUID fileId, FileOperationType operationType, FileUpdateOperation<T> operation) {
    Optional<StorageEntity> entity = metadataRepository.getFile(fileId);

    if (entity.isEmpty()) {
      throw new IllegalStateException(
          "FATAL: StorageEntity not found for id "
              + fileId
              + ". This implies a race condition or other critical error");
    }

    return executeUpdateOperation(entity.get(), operationType, operation);
  }

  public <T> T executeUpdateOperation(
      StorageEntity entity, FileOperationType operationType, FileUpdateOperation<T> operation) {
    prepareEntity(entity, operationType);

    try {
      T result = operation.apply(entity.getS3Key());
      finalizeEntityUpdate(entity, FileStatus.READY);
      return result;
    } catch (RecoverableStorageException e) {
      finalizeEntityUpdate(entity, FileStatus.ERROR, entity.getRetryCount() + 1);
      throw e;
    } catch (FatalStorageException e) {
      log.error("FATAL: Failed to perform operation {}", operationType);
      finalizeEntityUpdate(entity, FileStatus.FATAL);
      throw e;
    } catch (Exception e) {
      finalizeEntityUpdate(entity, FileStatus.READY);

      throw new RuntimeException(e);
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

  @FunctionalInterface
  public interface FileUpdateOperation<T> {
    T apply(String s3Key) throws Exception;
  }
}

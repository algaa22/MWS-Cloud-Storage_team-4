package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ChangeMetadataRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.UploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileLockedException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageRepositoryWrapper {
  private final StorageJpaRepositoryAdapter metadataRepository;
  private final TransactionTemplate transactionTemplate;
  private final StorageConfig storageConfig;
  private final RetryPolicy<Object> retryPolicy;
  private final EntityManager entityManager;

  public <T> void wrapUpdate(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsReady(entity);
    prepareEntityToPending(entity, operationType);
    finalizeOperation(entity, operationType, operation);
  }

  public <T> void wrapNewEntityTask(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    finalizeOperation(entity, operationType, operation);
  }

  public <T> T initiateStep(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsReady(entity);
    prepareEntityToPending(entity, operationType);
    return executeOperation(entity, operationType, operation);
  }

  public <T> T processStep(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsPending(entity);

    T result = executeOperation(entity, operationType, operation);

    if (shouldThrottledUpdate(entity)) {
      syncEntityWithDatabase(entity, operationType, FileStatus.PENDING);
    }

    return result;
  }

  public <T> void completeStep(
      StorageEntity entity, FileOperationType operationType, FileOperation<T> operation) {
    checkIfStatusIsPending(entity);
    finalizeOperation(entity, operationType, operation);
  }

  public void resetToReady(StorageEntity entity) {
    syncEntityWithDatabase(entity, FileOperationType.CHANGE_METADATA, FileStatus.READY, 0);
  }

  public String getFullFilePath(UUID fileId) {
    return transactionTemplate.execute(
        status -> {
          Optional<StorageEntity> entityOpt = metadataRepository.getFileById(fileId);
          StorageEntity entity =
              entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileId));
          return buildFullPath(entity);
        });
  }

  private String buildFullPath(StorageEntity entity) {
    StringBuilder path = new StringBuilder();
    buildPathRecursive(entity, path);
    return path.toString();
  }

  private void buildPathRecursive(StorageEntity entity, StringBuilder path) {
    if (entity.getParentId() != null) {
      metadataRepository
          .getFileById(entity.getParentId())
          .ifPresent(
              parent -> {
                buildPathRecursive(parent, path);
                path.append("/");
              });
    }
    path.append(entity.getName());
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
      default -> throw exception;
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

          StorageEntity managedEntity = getManagedEntity(entity);

          managedEntity.setStatus(newStatus);
          managedEntity.setRetryCount(newRetryCount);

          if (newStatus == FileStatus.PENDING || newStatus == FileStatus.READY) {
            managedEntity.setUpdatedAt(LocalDateTime.now());
          }

          if (newStatus == FileStatus.READY) {
            managedEntity.setOperationType(null);
          }

          metadataRepository.saveFile(managedEntity);
          return null;
        });
  }

  private StorageEntity getManagedEntity(StorageEntity entity) {
    StorageEntity managed = entityManager.find(StorageEntity.class, entity.getId());

    if (managed != null) {
      return entityManager.merge(entity);
    }

    String sql = "SELECT * FROM files WHERE id = :id";
    jakarta.persistence.Query query = entityManager.createNativeQuery(sql, StorageEntity.class);
    query.setParameter("id", entity.getId());

    @SuppressWarnings("unchecked")
    List<StorageEntity> result = query.getResultList();

    if (!result.isEmpty()) {
      StorageEntity fromDb = result.get(0);
      return entityManager.merge(fromDb);
    }

    entityManager.persist(entity);
    return entity;
  }

  @FunctionalInterface
  public interface FileOperation<T> {
    T apply() throws BaseStorageException;
  }
}

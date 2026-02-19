package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileMetadataRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepositoryWrapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {
  private final StorageRepositoryWrapper storageRepositoryWrapper;
  private final FileMetadataRepository metadataRepository;
  private final StorageRepository storageRepository;
  private final StorageConfig storageConfig;

  @Scheduled(cron = "17 42 * * * *")
  public void cleanupStaleFiles() {
    int staleTime = storageConfig.stateMachine().fileStaleTimeMin();
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleTime);
    List<StorageEntity> staleFiles = metadataRepository.getStaleFiles(threshold);

    if (staleFiles.isEmpty()) {
      return;
    }

    log.info("Found {} stale files", staleFiles.size());

    for (StorageEntity entity : staleFiles) {
      try {
        handleScaleFile(entity);
      } catch (Exception e) {
        log.error("Failed to cleanup file {}", entity.getId(), e);
      }
    }
  }

  // TODO: @Transactional
  private void handleScaleFile(StorageEntity entity) {
    switch (entity.getOperationType()) {
      case UPLOAD -> {
        storageRepository.deleteFile(entity);
        log.info("Cleanup: Deleted stale upload for file {}", entity.getId());
      }
      case DELETE -> {
        storageRepository.deleteFile(entity);
        log.info("Cleanup: Retried deletion for file {}", entity.getId());
      }
      case CHANGE_METADATA -> {
        storageRepositoryWrapper.forceRollbackOperation(entity);
        log.info("Cleanup: Forced rollback to stuck metadata operation for file {}", entity.getId());
      }
      default ->
          throw new IllegalStateException(
              "FATAL: Unknown operation type for file " + entity.getId());
    }
  }
}

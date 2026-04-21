package com.mipt.team4.cloud_storage_backend.schedulers.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageProps;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.utils.wrapper.BatchProcessor;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DangerousFileCleanupScheduler {
  private final StorageJpaRepositoryAdapter metadataRepository;
  private final BatchProcessor batchProcessor;
  private final StorageProps storageProps;

  private static final String TASK_NAME = "Dangerous File Cleanup";

  @Scheduled(cron = "${storage.scheduling.cron.dangerous-file-cleanup}")
  public void cleanupDangerousFiles() {
    int deletionTime = storageProps.scheduling().dangerousDeletionTimeDays();
    LocalDateTime threshold = LocalDateTime.now().minusDays(deletionTime);

    batchProcessor.scoop(
        TASK_NAME,
        pageable -> metadataRepository.getDangerousFiles(threshold, pageable),
        StorageEntity::getId,
        entity -> metadataRepository.hardDelete(entity.getUserId(), entity.getId()));
  }
}

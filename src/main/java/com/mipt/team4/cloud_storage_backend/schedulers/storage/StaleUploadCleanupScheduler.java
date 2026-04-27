package com.mipt.team4.cloud_storage_backend.schedulers.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageProps;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadSessionEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.ChunkedUploadJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.storage.ChunkedUploadService;
import com.mipt.team4.cloud_storage_backend.utils.wrapper.BatchProcessor;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaleUploadCleanupScheduler {
  private final ChunkedUploadJpaRepositoryAdapter uploadRepository;
  private final ChunkedUploadService uploadService;
  private final BatchProcessor batchProcessor;
  private final StorageProps storageProps;

  private static final String TASK_NAME = "Stale Upload Cleanup";

  @Scheduled(cron = "${storage.scheduling.cron.stale-upload-cleanup}")
  public void cleanupStaleUploads() {
    int staleTime = storageProps.scheduling().staleTimeMin().upload();
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleTime);

    batchProcessor.scoop(
        TASK_NAME,
        pageable -> uploadRepository.getStaleSessions(threshold, pageable),
        ChunkedUploadSessionEntity::getId,
        uploadService::forceAbortUpload);
  }
}

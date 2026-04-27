package com.mipt.team4.cloud_storage_backend.service.infrastructure;

import com.mipt.team4.cloud_storage_backend.config.constants.storage.StorageMetrics;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.repository.storage.ChunkedUploadJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepositoryAdapter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageGaugeMetrics {
  private final StorageJpaRepositoryAdapter metadataRepository;
  private final ChunkedUploadJpaRepositoryAdapter uploadRepository;
  private final MeterRegistry meterRegistry;

  @PostConstruct
  public void registerMetrics() {
    registerFilesStatusCount();
    registerUploadChunksActive();
  }

  private void registerFilesStatusCount() {
    for (FileStatus status : FileStatus.values()) {
      Gauge.builder(
              StorageMetrics.FILES_STATUS_COUNT, () -> metadataRepository.countByStatus(status))
          .tag("status", status.name())
          .description("Current count of file in " + status)
          .register(meterRegistry);
    }
  }

  private void registerUploadChunksActive() {
    Gauge.builder(StorageMetrics.UPLOAD_CHUNKS_ACTIVE, uploadRepository::count)
        .description("Current count of active upload sessions")
        .register(meterRegistry);
  }
}

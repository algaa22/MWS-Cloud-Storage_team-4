package com.mipt.team4.cloud_storage_backend.schedulers.storage;

import com.mipt.team4.cloud_storage_backend.antivirus.service.AntivirusService;
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
public class StaleScanScheduler {
  private final StorageJpaRepositoryAdapter metadataRepository;
  private final AntivirusService antivirusService;
  private final BatchProcessor batchProcessor;
  private final StorageProps storageProps;

  private static final String TASK_NAME = "Stale Scan Scheduler";

  @Scheduled(cron = "${storage.scheduling.cron.stale-scanning}")
  public void handleStaleScans() {
    int staleTime = storageProps.scheduling().staleTimeMin().scan();
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleTime);

    batchProcessor.scroll(
        TASK_NAME,
        pageable -> metadataRepository.getStaleScans(threshold, pageable),
        StorageEntity::getId,
        antivirusService::handleStaleScan);
  }
}

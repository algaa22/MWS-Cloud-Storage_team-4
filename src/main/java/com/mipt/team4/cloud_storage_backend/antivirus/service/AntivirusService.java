package com.mipt.team4.cloud_storage_backend.antivirus.service;

import com.mipt.team4.cloud_storage_backend.antivirus.config.props.AntivirusProps;
import com.mipt.team4.cloud_storage_backend.antivirus.messaging.AntivirusTaskProducer;
import com.mipt.team4.cloud_storage_backend.antivirus.model.dto.ScanResultDto;
import com.mipt.team4.cloud_storage_backend.antivirus.model.enums.ScanVerdict;
import com.mipt.team4.cloud_storage_backend.antivirus.model.exception.ScannedFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.antivirus.model.exception.ScannedFileOwnerNotFoundException;
import com.mipt.team4.cloud_storage_backend.antivirus.model.mapper.ScanTaskMapper;
import com.mipt.team4.cloud_storage_backend.config.constants.storage.StorageMetrics;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.NotificationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AntivirusService {
  private final NotificationService notificationService;
  private final StorageRepository storageRepository;
  private final UserJpaRepositoryAdapter userRepository;
  private final AntivirusTaskProducer taskProducer;
  private final AntivirusProps antivirusProps;
  private final MeterRegistry meterRegistry;

  @Transactional(propagation = Propagation.MANDATORY)
  public void sendToScan(StorageEntity fileEntity) {
    if (!antivirusProps.enabled()) {
      log.debug("Antivirus is disabled. Skipping scan for file: {}", fileEntity.getId());
      return;
    }

    log.debug(
        "Preparing to send file to scan. ID: {}, Name: {}, UserID: {}",
        fileEntity.getId(),
        fileEntity.getName(),
        fileEntity.getUserId());

    fileEntity.setScanVerdict(ScanVerdict.SCANNING);

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            log.info(
                "Transaction committed. Sending task to RabbitMQ for fileId: {}",
                fileEntity.getId());
            taskProducer.sendTask(ScanTaskMapper.toTask(fileEntity));
          }
        });
  }

  @Transactional
  public void handleScanResult(ScanResultDto result) {
    log.info(
        "Received scan result from worker: fileId={}, verdict={}",
        result.fileId(),
        result.verdict());

    StorageEntity fileEntity =
        storageRepository
            .get(result.fileId())
            .orElseThrow(
                () -> {
                  log.error(
                      "CRITICAL: Scanned file NOT FOUND in database! fileId: {}. Check for ID mismatch or rollback.",
                      result.fileId());
                  return new ScannedFileNotFoundException(result.fileId());
                });
    fileEntity.setScanVerdict(result.verdict());

    log.debug(
        "Found entity for scan result. Current status: {}, S3Key: {}",
        fileEntity.getStatus(),
        fileEntity.getS3Key());

    UserEntity userEntity =
        userRepository
            .getUserById(fileEntity.getUserId())
            .orElseThrow(
                () -> {
                  log.error(
                      "Owner not found for file: {}. userId: {}",
                      fileEntity.getId(),
                      fileEntity.getUserId());
                  return new ScannedFileOwnerNotFoundException(fileEntity.getUserId());
                });

    incrementScanCount(result.verdict());

    if (result.verdict().isCritical()) {
      log.warn(
          "Dangerous file detected! ID: {}, Verdict: {}. Initiating cleanup.",
          fileEntity.getId(),
          result.verdict());
      handleDangerousFile(fileEntity, userEntity);
      return;
    }

    if (result.verdict() == ScanVerdict.ERROR) {
      log.error("Antivirus worker reported an error for file: {}", fileEntity.getId());
      notificationService.notifyScanError(fileEntity, userEntity);
    }

    log.info("Scan successful for file: {}.", fileEntity.getId());
  }

  @Transactional
  public void handleStaleScan(StorageEntity fileEntity) {
    log.warn("Handling stale scan for file: {}. Marking as ERROR.", fileEntity.getId());
    fileEntity.setScanVerdict(ScanVerdict.ERROR);

    UserEntity userEntity =
        userRepository
            .getUserById(fileEntity.getUserId())
            .orElseThrow(() -> new UserNotFoundException(fileEntity.getUserId()));
    notificationService.notifyScanError(fileEntity, userEntity);
  }

  private void handleDangerousFile(StorageEntity fileEntity, UserEntity userEntity) {
    storageRepository.cleanupDangerousFile(fileEntity);
    notificationService.notifyDangerousFile(fileEntity, userEntity);
    log.debug("Dangerous file cleanup completed for ID: {}", fileEntity.getId());
  }

  private void incrementScanCount(ScanVerdict verdict) {
    Counter.builder(StorageMetrics.ANTIVIRUS_SCAN_COUNT)
        .tag("verdict", verdict.name())
        .tag("critical", String.valueOf(verdict.isCritical()))
        .register(meterRegistry)
        .increment();
  }
}

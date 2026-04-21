package com.mipt.team4.cloud_storage_backend.antivirus.service;

import com.mipt.team4.cloud_storage_backend.antivirus.config.props.AntivirusProps;
import com.mipt.team4.cloud_storage_backend.antivirus.messaging.AntivirusTaskProducer;
import com.mipt.team4.cloud_storage_backend.antivirus.model.dto.ScanResultDto;
import com.mipt.team4.cloud_storage_backend.antivirus.model.enums.ScanVerdict;
import com.mipt.team4.cloud_storage_backend.antivirus.model.exception.ScannedFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.antivirus.model.exception.ScannedFileOwnerNotFoundException;
import com.mipt.team4.cloud_storage_backend.antivirus.model.mapper.ScanTaskMapper;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AntivirusService {
  private final NotificationService notificationService;
  private final StorageRepository storageRepository;
  private final UserJpaRepositoryAdapter userRepository;
  private final AntivirusTaskProducer taskProducer;
  private final AntivirusProps antivirusProps;

  @Transactional(propagation = Propagation.MANDATORY)
  public void sendToScan(StorageEntity fileEntity) {
    if (!antivirusProps.enabled()) {
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            taskProducer.sendTask(ScanTaskMapper.toTask(fileEntity));
          }
        });
  }

  @Transactional
  public void handleScanResult(ScanResultDto result) {
    StorageEntity fileEntity =
        storageRepository
            .get(result.fileId())
            .orElseThrow(() -> new ScannedFileNotFoundException(result.fileId()));

    UserEntity userEntity =
        userRepository
            .getUserById(fileEntity.getUserId())
            .orElseThrow(() -> new ScannedFileOwnerNotFoundException(fileEntity.getUserId()));

    if (result.verdict().isCritical()) {
      handleDangerousFile(fileEntity, userEntity);
      return;
    }

    if (result.verdict() == ScanVerdict.ERROR) {
      notificationService.notifyScanError(fileEntity, userEntity);
    }

    fileEntity.setStatus(FileStatus.READY);
  }

  @Transactional
  public void handleStaleScan(StorageEntity fileEntity) {
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
  }
}

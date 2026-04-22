package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.config.props.NotificationConfig;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
  private final UserJpaRepositoryAdapter userRepository;
  private final NotificationClient notificationClient;
  private final NotificationConfig notificationConfig;
  private final StorageRepository storageRepository;

  @Transactional(readOnly = true)
  public void notifyFileDeleted(UUID fileId, UserEntity userEntity) {
    String fullPath = storageRepository.getFullFilePath(fileId);
    notificationClient.notifyFileDeleted(
        userEntity.getEmail(), userEntity.getUsername(), fullPath, userEntity.getId());
  }

  @Transactional(readOnly = true)
  public void notifyDangerousFile(StorageEntity fileEntity, UserEntity userEntity) {
    String folderPath = storageRepository.getFullFolderPath(fileEntity);
    notificationClient.notifyDangerousFile(
        userEntity.getEmail(),
        userEntity.getUsername(),
        fileEntity.getName(),
        folderPath,
        fileEntity.getScanVerdict().name(),
        userEntity.getId());
  }

  @Transactional(readOnly = true)
  public void notifyScanError(StorageEntity fileEntity, UserEntity userEntity) {
    String folderPath = storageRepository.getFullFolderPath(fileEntity);
    notificationClient.notifyScanError(
        userEntity.getEmail(),
        userEntity.getUsername(),
        fileEntity.getName(),
        folderPath,
        userEntity.getId());
  }

  @Transactional(readOnly = true)
  public void checkStorageUsageAndNotify(UUID userId) {
    userRepository
        .getStorageUsage(userId)
        .ifPresent(
            usage -> {
              double ratio = usage.getRatio();

              log.info(
                  "Storage check for user {}: used={}, limit={}, {}%",
                  userId, usage.used(), usage.limit(), String.format("%.2f", ratio * 100));

              if (ratio >= notificationConfig.fullThreshold()) {
                userRepository
                    .getUserById(userId)
                    .ifPresent(
                        user ->
                            notificationClient.notifyStorageFull(
                                user.getEmail(), user.getUsername(), userId));
              } else if (ratio >= notificationConfig.almostFullThreshold()) {
                userRepository
                    .getUserById(userId)
                    .ifPresent(
                        user ->
                            notificationClient.notifyStorageAlmostFull(
                                user.getEmail(),
                                user.getUsername(),
                                usage.used(),
                                usage.limit(),
                                userId));
              }
            });
  }
}

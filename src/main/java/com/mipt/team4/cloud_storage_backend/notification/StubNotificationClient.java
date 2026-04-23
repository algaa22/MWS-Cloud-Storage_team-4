package com.mipt.team4.cloud_storage_backend.notification;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class StubNotificationClient implements NotificationClient {

  @Override
  public void notifyFileDeleted(String userEmail, String userName, String fileName, UUID userId) {}

  @Override
  public void notifyStorageAlmostFull(
      String userEmail, String userName, long usedStorage, long storageLimit, UUID userId) {}

  @Override
  public void notifyStorageFull(String userEmail, String userName, UUID userId) {}

  @Override
  public void notifyTariffPurchased(
      String email, String name, String tariffName, LocalDateTime endDate) {}

  @Override
  public void notifyTariffEndingSoon(
      String email, String name, int daysLeft, LocalDateTime endDate) {}

  @Override
  public void notifyTariffExpired(String email, String name) {}

  @Override
  public void notifyTariffRenewed(
      String email, String name, String tariffName, LocalDateTime newEndDate) {}

  @Override
  public void notifySubscriptionExpired(String email, String name, LocalDateTime deletionDate) {}

  @Override
  public void notifyFilesDeleted(String email, String name) {}

  @Override
  public void notifyTrialExpired(String email, String name) {}

  @Override
  public void notifyTrialStarted(String email, String name, LocalDateTime trialEndDate) {}

  public void notifyTariffRenewed(String email, String name, LocalDateTime newEndDate) {}

  @Override
  public void notifyDangerousFile(
      String userEmail,
      String userName,
      String fileName,
      String folderPath,
      String verdict,
      UUID userId) {}

  @Override
  public void notifyScanError(
      String userEmail, String userName, String fileName, String folderPath, UUID userId) {}
}

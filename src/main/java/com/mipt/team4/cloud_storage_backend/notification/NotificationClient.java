package com.mipt.team4.cloud_storage_backend.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationClient {
  void notifyFileDeleted(String userEmail, String userName, String fileName, UUID userId);

  void notifyStorageAlmostFull(
      String userEmail, String userName, long usedStorage, long storageLimit, UUID userId);

  void notifyStorageFull(String userEmail, String userName, UUID userId);

  void notifyTariffPurchased(String email, String name, String tariffName, LocalDateTime endDate);

  void notifyTariffEndingSoon(String email, String name, int daysLeft, LocalDateTime endDate);

  void notifyTariffExpired(String email, String name);

  void notifyTariffRenewed(String email, String name, String tariffName, LocalDateTime newEndDate);

  void notifySubscriptionExpired(String email, String name, LocalDateTime deletionDate);

  void notifyFilesDeleted(String email, String name);

  void notifyTrialExpired(String email, String name);

  void notifyTrialStarted(String email, String name, LocalDateTime trialEndDate);
}

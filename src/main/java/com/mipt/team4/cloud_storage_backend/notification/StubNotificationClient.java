package com.mipt.team4.cloud_storage_backend.notification;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StubNotificationClient implements NotificationClient {

  @Override
  public void notifyFileDeleted(String userEmail, String userName, String fileName, UUID userId) {
    log.info("STUB: File deleted notification for {} ({}): {}", userName, userEmail, fileName);
  }

  @Override
  public void notifyStorageAlmostFull(
      String userEmail, String userName, long usedStorage, long storageLimit, UUID userId) {
    log.info(
        "STUB: Storage almost full for {} ({}): {}/{}",
        userName,
        userEmail,
        usedStorage,
        storageLimit);
  }

  @Override
  public void notifyStorageFull(String userEmail, String userName, UUID userId) {
    log.info("STUB: Storage full for {} ({})", userName, userEmail);
  }

  @Override
  public void notifyTariffPurchased(
      String email, String name, String tariffName, LocalDateTime endDate) {
    log.info("STUB: Tariff purchased for {} ({}): {}, ends {}", name, email, tariffName, endDate);
  }

  @Override
  public void notifyTariffEndingSoon(
      String email, String name, int daysLeft, LocalDateTime endDate) {
    log.info(
        "STUB: Tariff ending soon for {} ({}): {} days left, ends {}",
        name,
        email,
        daysLeft,
        endDate);
  }

  @Override
  public void notifyTariffExpired(String email, String name) {
    log.info("STUB: Tariff expired for {} ({})", name, email);
  }

  @Override
  public void notifyTariffRenewed(
      String email, String name, String tariffName, LocalDateTime newEndDate) {
    log.info(
        "STUB: Tariff renewed for {} ({}): {}, new end date {}",
        name,
        email,
        tariffName,
        newEndDate);
  }

  @Override
  public void notifySubscriptionExpired(String email, String name, LocalDateTime deletionDate) {
    log.info(
        "STUB: Subscription expired for {} ({}), files will be deleted after {}",
        name,
        email,
        deletionDate);
  }

  @Override
  public void notifyFilesDeleted(String email, String name) {
    log.info("STUB: Files deleted for {} ({})", name, email);
  }

  @Override
  public void notifyTrialExpired(String email, String name) {
    log.info("STUB: Trial expired for {} ({})", name, email);
  }

  @Override
  public void notifyTrialStarted(String email, String name, LocalDateTime trialEndDate) {
    log.info("STUB: Trial started for {} ({}), ends at {}", name, email, trialEndDate);
  }
}

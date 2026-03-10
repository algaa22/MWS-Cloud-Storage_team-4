package com.mipt.team4.cloud_storage_backend.scheduler;

import com.mipt.team4.cloud_storage_backend.config.props.TariffNotificationConfig;
import com.mipt.team4.cloud_storage_backend.exception.user.PaymentException;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.PaymentService;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TariffScheduler {

  private final UserRepository userRepository;
  private final NotificationClient notificationClient;
  private final PaymentService paymentService;
  private final TariffNotificationConfig notificationConfig;

  @Scheduled(cron = "0 0 4 * * *")
  public void checkTariffs() {
    if (!notificationConfig.isEnabled()) {
      log.debug("Tariff notifications are disabled");
      return;
    }

    log.info("Starting scheduled tariff check");

    List<Integer> notificationDays = notificationConfig.getDaysBeforeExpiry();

    for (int days : notificationDays) {
      checkUsersWithTariffEndingSoon(days);
    }

    checkExpiredTariffs();

    log.info("Scheduled tariff check completed");
  }

  private void checkUsersWithTariffEndingSoon(int days) {
    LocalDateTime from = LocalDateTime.now().plusDays(days);
    LocalDateTime to = from.plusDays(1);

    List<UserEntity> users = userRepository.findUsersWithTariffEndingBetween(from, to);

    if (users.isEmpty()) {
      log.debug("No users with tariff ending in {} days", days);
      return;
    }

    log.info("Found {} users with tariff ending in {} days", users.size(), days);

    for (UserEntity user : users) {
      try {
        notificationClient.notifyTariffEndingSoon(
            user.getEmail(), user.getName(), days, user.getTariffEndDate());
        log.info("Notified user {}: tariff ends in {} days", user.getId(), days);
      } catch (Exception e) {
        log.error("Failed to notify user {} about tariff ending in {} days", user.getId(), days, e);
      }
    }
  }

  private void checkExpiredTariffs() {
    List<UserEntity> expiredUsers = userRepository.findUsersWithExpiredTariff(LocalDateTime.now());

    if (expiredUsers.isEmpty()) {
      log.debug("No users with expired tariffs");
      return;
    }

    log.info("Found {} users with expired tariffs", expiredUsers.size());

    for (UserEntity user : expiredUsers) {
      try {
        if (user.isAutoRenew()) {
          handleAutoRenew(user);
        } else {
          deactivateUser(user);
        }
      } catch (Exception e) {
        log.error("Failed to process expired tariff for user {}", user.getId(), e);
      }
    }
  }

  private void handleAutoRenew(UserEntity user) {
    log.info("Processing auto-renew for user: {}", user.getId());

    try {
      paymentService.autoRenewTariff(user.getId());

      LocalDateTime newEndDate =
          LocalDateTime.now().plusDays(user.getTariffPlan().getDurationDays());
      userRepository.updateTariffEndDate(user.getId(), newEndDate);

      notificationClient.notifyTariffRenewed(user.getEmail(), user.getName(), newEndDate);

      log.info("Auto-renew successful for user: {}, new end date: {}", user.getId(), newEndDate);

    } catch (PaymentException e) {
      log.error("Auto-renew failed for user: {}", user.getId(), e);
      deactivateUser(user);
    }
  }

  private void deactivateUser(UserEntity user) {
    log.info("Deactivating user {} due to expired tariff", user.getId());

    userRepository.deactivateUser(user.getId());

    try {
      notificationClient.notifyTariffExpired(user.getEmail(), user.getName());
    } catch (Exception e) {
      log.error("Failed to send expiration notification to user {}", user.getId(), e);
    }

    log.info("User {} deactivated due to expired tariff", user.getId());
  }
}

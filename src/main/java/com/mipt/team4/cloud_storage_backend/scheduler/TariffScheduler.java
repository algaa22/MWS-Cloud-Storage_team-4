package com.mipt.team4.cloud_storage_backend.scheduler;

import com.mipt.team4.cloud_storage_backend.config.props.NotificationConfig;
import com.mipt.team4.cloud_storage_backend.exception.user.payment.PaymentException;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.UserStatus;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.PaymentService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TariffScheduler {

  private static final int GRACE_PERIOD_DAYS = 30;
  private final UserJpaRepositoryAdapter userRepository;
  private final NotificationClient notificationClient;
  private final PaymentService paymentService;
  private final NotificationConfig notificationConfig;

  @Scheduled(cron = "0 0 4 * * *")
  @Transactional
  public void checkTariffs() {
    log.info("Starting scheduled tariff check");

    checkUsersWithTariffEndingSoon();

    checkExpiredTariffs();

    checkUsersInGracePeriod();

    checkExpiredTrials();

    log.info("Scheduled tariff check completed");
  }

  private void checkUsersWithTariffEndingSoon() {
    List<Integer> notificationDays = notificationConfig.tariff().daysBeforeExpiry();

    for (int days : notificationDays) {
      LocalDateTime from = LocalDateTime.now().plusDays(days);
      LocalDateTime to = from.plusDays(1);

      List<UserEntity> users = userRepository.getUsersWithTariffEndingBetween(from, to);

      if (users.isEmpty()) {
        log.debug("No users with tariff ending in {} days", days);
        continue;
      }

      log.info("Found {} users with tariff ending in {} days", users.size(), days);

      for (UserEntity user : users) {
        try {
          notificationClient.notifyTariffEndingSoon(
              user.getEmail(), user.getUsername(), days, user.getTariffEndDate());
          log.debug("Notified user {}: tariff ends in {} days", user.getId(), days);
        } catch (Exception e) {
          log.error(
              "Failed to notify user {} about tariff ending in {} days", user.getId(), days, e);
        }
      }
    }
  }

  private void checkExpiredTariffs() {
    LocalDateTime now = LocalDateTime.now();
    List<UserEntity> expiredUsers = userRepository.getUsersWithExpiredTariff(now);

    if (expiredUsers.isEmpty()) {
      log.debug("No users with expired tariffs");
      return;
    }

    log.info("Found {} users with expired tariffs", expiredUsers.size());

    for (UserEntity user : expiredUsers) {
      try {
        if (user.isAutoRenew() && user.getPaymentMethodId() != null) {
          handleAutoRenew(user);
        } else {
          restrictUserAccess(user);
        }
      } catch (Exception e) {
        log.error("Failed to process expired tariff for user {}", user.getId(), e);
        restrictUserAccess(user);
      }
    }
  }

  /** Обработка автопродления тарифа */
  private void handleAutoRenew(UserEntity user) {
    log.info("Processing auto-renew for user: {}", user.getId());

    try {
      paymentService.processAutoRenewal(user.getId(), user.getTariffPlan());

      LocalDateTime newEndDate =
          LocalDateTime.now().plusDays(user.getTariffPlan().getDurationDays());

      userRepository.updateTariffEndDate(user.getId(), newEndDate);

      notificationClient.notifyTariffRenewed(
          user.getEmail(), user.getUsername(), user.getTariffPlan().name(), newEndDate);

      log.info("Auto-renew successful for user: {}, new end date: {}", user.getId(), newEndDate);

    } catch (PaymentException e) {
      log.error("Auto-renew failed for user: {}", user.getId(), e);
      restrictUserAccess(user);
    }
  }

  /** Ограничение доступа пользователя при просрочке тарифа */
  private void restrictUserAccess(UserEntity user) {
    log.info("Restricting access for user {} due to expired tariff", user.getId());

    userRepository.updateUserStatus(user.getId(), UserStatus.RESTRICTED);

    LocalDateTime deletionDate = LocalDateTime.now().plusDays(GRACE_PERIOD_DAYS);
    userRepository.updateScheduledDeletionDate(user.getId(), deletionDate);

    try {
      notificationClient.notifySubscriptionExpired(
          user.getEmail(), user.getUsername(), deletionDate);
    } catch (Exception e) {
      log.error("Failed to send expiration notification to user {}", user.getId(), e);
    }

    log.info("User {} restricted, scheduled deletion: {}", user.getId(), deletionDate);
  }

  /** Проверка пользователей в грас-периоде Удаляет файлы у тех, у кого истек срок ожидания */
  private void checkUsersInGracePeriod() {
    LocalDateTime now = LocalDateTime.now();

    List<UserEntity> usersToDelete =
        userRepository.findAllByUserStatusAndScheduledDeletionDateBefore(
            UserStatus.RESTRICTED, now);

    if (usersToDelete.isEmpty()) {
      log.debug("No users pending deletion");
      return;
    }

    log.info("Found {} users pending deletion", usersToDelete.size());

    for (UserEntity user : usersToDelete) {
      try {

        userRepository.updateTariff(user.getId(), null, null, null, false, null);
        userRepository.updateUserStatus(user.getId(), UserStatus.ACTIVE);
        userRepository.updateScheduledDeletionDate(user.getId(), null);

        userRepository.updateTrialDates(user.getId(), null, null);

        notificationClient.notifyFilesDeleted(user.getEmail(), user.getUsername());

        log.info("Successfully processed deletion for user: {}", user.getId());

      } catch (Exception e) {
        log.error("Failed to process deletion for user: {}", user.getId(), e);
      }
    }
  }

  /** Проверка истекших пробных периодов */
  private void checkExpiredTrials() {
    LocalDateTime now = LocalDateTime.now();

    List<UserEntity> expiredTrialUsers =
        userRepository.findAllByTrialEndDateBeforeAndTariffPlanIsNull(now);

    if (expiredTrialUsers.isEmpty()) {
      log.debug("No users with expired trials");
      return;
    }

    log.info("Found {} users with expired trials", expiredTrialUsers.size());

    for (UserEntity user : expiredTrialUsers) {
      try {
        userRepository.updateTrialDates(user.getId(), null, null);

        notificationClient.notifyTrialExpired(user.getEmail(), user.getUsername());

        log.info("Trial period expired for user: {}", user.getId());

      } catch (Exception e) {
        log.error("Failed to process expired trial for user: {}", user.getId(), e);
      }
    }
  }
}

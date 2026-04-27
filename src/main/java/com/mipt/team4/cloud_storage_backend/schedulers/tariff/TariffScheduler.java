package com.mipt.team4.cloud_storage_backend.schedulers.tariff;

import com.mipt.team4.cloud_storage_backend.config.props.NotificationProps;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.UserStatus;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.utils.wrapper.BatchProcessor;
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

  private final UserJpaRepositoryAdapter userRepository;
  private final NotificationClient notificationClient;
  private final NotificationProps notificationConfig;
  private final TariffService tariffService;
  private final BatchProcessor batchProcessor;

  private static final String TASK_NAME = "Tariff Scheduler";

  @Scheduled(cron = "${storage.scheduling.cron.check-tariff}")
  public void checkTariffs() {
    log.info("[{}] Starting", TASK_NAME);

    List<Integer> notificationDays = notificationConfig.tariff().daysBeforeExpiry();

    for (int days : notificationDays) {
      checkUsersWithTariffEndingSoon(days);
    }

    checkExpiredTariffs();
    checkUsersInGracePeriod();
    checkExpiredTrials();

    log.info("[{}] Completed", TASK_NAME);
  }

  private void checkUsersWithTariffEndingSoon(int days) {
    LocalDateTime from = LocalDateTime.now().plusDays(days);
    LocalDateTime to = from.plusDays(1);

    batchProcessor.scroll(
        TASK_NAME + ": Notification " + days + "d",
        pageable -> userRepository.getUsersWithTariffEndingBetween(from, to, pageable),
        UserEntity::getId,
        user ->
            notificationClient.notifyTariffEndingSoon(
                user.getEmail(), user.getUsername(), days, user.getTariffEndDate()));
  }

  private void checkExpiredTariffs() {
    batchProcessor.scoop(
        TASK_NAME + ": Expiry",
        pageable -> userRepository.getUsersWithExpiredTariff(LocalDateTime.now(), pageable),
        UserEntity::getId,
        user -> {
          if (user.isAutoRenew() && user.getPaymentMethodId() != null) {
            tariffService.handleAutoRenew(user);
          } else {
            tariffService.restrictUserAccess(user);
          }
        });
  }

  private void checkUsersInGracePeriod() {
    batchProcessor.scoop(
        TASK_NAME + ": Grace Period",
        pageable ->
            userRepository.findAllByUserStatusAndScheduledDeletionDateBefore(
                UserStatus.RESTRICTED, LocalDateTime.now(), pageable),
        UserEntity::getId,
        tariffService::checkUsersInGracePeriod);
  }

  private void checkExpiredTrials() {
    batchProcessor.scoop(
        TASK_NAME + ": Trial Expiry",
        pageable ->
            userRepository.findAllByTrialEndDateBeforeAndTariffPlanIsNull(
                LocalDateTime.now(), pageable),
        UserEntity::getId,
        tariffService::checkExpiredTrials);
  }
}

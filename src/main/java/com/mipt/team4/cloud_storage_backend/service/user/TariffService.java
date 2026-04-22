package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.payment.dto.PaymentHistoryResponse;
import com.mipt.team4.cloud_storage_backend.model.payment.dto.PaymentTransactionDto;
import com.mipt.team4.cloud_storage_backend.model.payment.entity.PaymentTransaction;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TariffInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.PurchaseTariffRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SetAutoRenewRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.TariffInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdatePaymentMethodRequest;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.model.user.enums.UserStatus;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.payment.PaymentTransactionRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.storage.FileCleanupService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TariffService {

  private static final long FREE_STORAGE_LIMIT = 5L * 1024 * 1024 * 1024;
  private static final int TRIAL_DAYS = 30;
  private final UserJpaRepositoryAdapter userRepository;
  private final NotificationClient notificationClient;
  private final PaymentService paymentService;
  private final FileCleanupService fileCleanupService;
  private final PaymentTransactionRepository paymentTransactionRepository;

  @Transactional
  public void setupTrialPeriod(UUID userId) {
    UserEntity user =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    if (user.getTariffPlan() != null) {
      log.info("User {} already purchased a tariff before, no trial period", userId);
      return;
    }

    if (user.getTrialStartDate() != null) {
      log.info("User {} already had a trial period", userId);
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime trialEndDate = now.plusDays(TRIAL_DAYS);

    userRepository.updateTrialDates(userId, now, trialEndDate);
    log.info("Trial period started for user: {}, ends at: {}", userId, trialEndDate);
  }

  @Transactional
  public void purchaseTariff(PurchaseTariffRequest request) {
    UUID userId = request.userId();
    UserEntity userEntity =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    TariffPlan plan = request.plan();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endDate = now.plusDays(plan.getDurationDays());

    boolean isFirstPurchase = userEntity.getTariffPlan() == null;

    BigDecimal amount = getTariffPrice(plan);
    String paymentToken =
        request.paymentToken() != null ? request.paymentToken() : "test_token_" + UUID.randomUUID();

    PaymentTransaction transaction =
        PaymentTransaction.builder()
            .userId(userId)
            .tariffPlan(plan.name())
            .amount(amount)
            .paymentToken(paymentToken)
            .status("PENDING")
            .paymentMethod(request.paymentMethod())
            .autoRenew(request.autoRenew())
            .storageLimitGb(plan.getStorageLimit() / (1024 * 1024 * 1024))
            .price(amount)
            .durationDays(plan.getDurationDays())
            .build();

    paymentTransactionRepository.save(transaction);
    log.info(
        "Created payment transaction for user: {}, plan: {}, id: {}",
        userId,
        plan.name(),
        transaction.getId());

    try {
      paymentService.processPayment(userId, plan, paymentToken);

      transaction.complete();
      paymentTransactionRepository.save(transaction);
      log.info("Payment transaction completed: {}", transaction.getId());

      userRepository.updateTariff(
          userId, plan, now, endDate, request.autoRenew(), plan.getStorageLimit());

      if (isFirstPurchase && userEntity.getTrialEndDate() != null) {
        userRepository.updateTrialDates(userId, null, null);
      }

      if (request.paymentMethod() != null) {
        userRepository.updatePaymentMethod(userId, request.paymentMethod());
      }

      if (userEntity.getUserStatus() != UserStatus.ACTIVE) {
        userRepository.updateUserStatus(userId, UserStatus.ACTIVE);
        userRepository.updateScheduledDeletionDate(userId, null);
      }

      notificationClient.notifyTariffPurchased(
          userEntity.getEmail(), userEntity.getUsername(), plan.name(), endDate);

      log.info("User {} purchased tariff: {}", userId, plan.name());

    } catch (Exception e) {
      transaction.fail();
      paymentTransactionRepository.save(transaction);
      log.error("Payment failed for user: {}, plan: {}", userId, plan.name(), e);
      throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
    }
  }

  @Transactional
  public void setAutoRenew(SetAutoRenewRequest request) {
    UUID userId = request.userId();
    boolean enabled = request.enabled();

    userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    userRepository.updateAutoRenew(userId, enabled);

    log.info("Auto-renew {} for user: {}", enabled ? "enabled" : "disabled", userId);
  }

  @Transactional
  public void updatePaymentMethod(UpdatePaymentMethodRequest request) {
    UUID userId = request.userId();

    userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    userRepository.updatePaymentMethod(userId, request.paymentMethodId());

    log.info("Payment method updated for user: {}", userId);
  }

  @Transactional(readOnly = true)
  public TariffInfoDto getTariffInfo(TariffInfoRequest request) {
    UUID userId = request.userId();
    UserEntity userEntity =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    TariffPlan tariffPlan = userEntity.getTariffPlan();
    LocalDateTime endDate = userEntity.getTariffEndDate();

    int daysLeft = 0;
    if (endDate != null && endDate.isAfter(LocalDateTime.now())) {
      daysLeft = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), endDate);
    }

    boolean hasActiveTrial =
        userEntity.getTariffPlan() == null
            && userEntity.getTrialEndDate() != null
            && userEntity.getTrialEndDate().isAfter(LocalDateTime.now());

    return new TariffInfoDto(
        tariffPlan,
        userEntity.getTotalStorageLimit(),
        userEntity.getUsedStorage(),
        userEntity.getFreeStorageLimit(),
        userEntity.getTariffStartDate(),
        endDate,
        userEntity.isAutoRenew(),
        userEntity.getUserStatus() == UserStatus.ACTIVE,
        daysLeft,
        hasActiveTrial,
        userEntity.getTrialEndDate());
  }

  @Transactional(readOnly = true)
  public boolean canModifyFiles(UUID userId) {
    UserEntity userEntity =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    return userEntity.getUserStatus() == UserStatus.ACTIVE;
  }

  @Transactional(readOnly = true)
  public PaymentHistoryResponse getPaymentHistory(UUID userId) {
    userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    List<PaymentTransaction> transactions =
        paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);

    List<PaymentTransactionDto> dtos =
        transactions.stream().map(PaymentTransactionDto::from).toList();

    log.info("Retrieved {} payment transactions for user: {}", dtos.size(), userId);
    return PaymentHistoryResponse.of(dtos);
  }

  private BigDecimal getTariffPrice(TariffPlan plan) {
    return BigDecimal.valueOf(plan.getPriceRub());
  }

  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void checkExpiredSubscriptions() {
    LocalDateTime now = LocalDateTime.now();

    List<UserEntity> expiredUsers =
        userRepository.findAllByTariffEndDateBeforeAndUserStatus(now, UserStatus.ACTIVE);

    for (UserEntity user : expiredUsers) {
      if (user.isAutoRenew() && user.getPaymentMethodId() != null) {
        try {
          paymentService.processAutoRenewal(user.getId(), user.getTariffPlan());
          LocalDateTime newEndDate = now.plusDays(user.getTariffPlan().getDurationDays());
          userRepository.updateTariffEndDate(user.getId(), newEndDate);
          notificationClient.notifyTariffRenewed(
              user.getEmail(), user.getUsername(), user.getTariffPlan().name(), newEndDate);
          continue;
        } catch (Exception e) {
          log.error("Auto-renewal failed for user: {}", user.getId(), e);
        }
      }

      userRepository.updateUserStatus(user.getId(), UserStatus.RESTRICTED);

      LocalDateTime deletionDate = now.plusDays(30);
      userRepository.updateScheduledDeletionDate(user.getId(), deletionDate);

      notificationClient.notifySubscriptionExpired(
          user.getEmail(), user.getUsername(), deletionDate);
      log.info(
          "User {} subscription expired, restricted access, scheduled deletion: {}",
          user.getId(),
          deletionDate);
    }
  }

  @Scheduled(cron = "0 0 1 * * *")
  @Transactional
  public void processPendingDeletions() {
    LocalDateTime now = LocalDateTime.now();

    List<UserEntity> usersToDelete =
        userRepository.findAllByUserStatusAndScheduledDeletionDateBefore(
            UserStatus.RESTRICTED, now);

    for (UserEntity user : usersToDelete) {
      long filesToDeleteSize = user.getUsedStorage() - FREE_STORAGE_LIMIT;
      if (filesToDeleteSize > 0) {
        fileCleanupService.deleteOldestFiles(user.getId(), filesToDeleteSize);
      }

      userRepository.updateTariff(user.getId(), null, null, null, false, null);
      userRepository.updateUserStatus(user.getId(), UserStatus.ACTIVE);
      userRepository.updateScheduledDeletionDate(user.getId(), null);

      notificationClient.notifyFilesDeleted(user.getEmail(), user.getUsername());
    }
  }

  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void checkExpiredTrials() {
    LocalDateTime now = LocalDateTime.now();

    List<UserEntity> expiredTrialUsers =
        userRepository.findAllByTrialEndDateBeforeAndTariffPlanIsNull(now);

    for (UserEntity user : expiredTrialUsers) {
      userRepository.updateTrialDates(user.getId(), null, null);

      notificationClient.notifyTrialExpired(user.getEmail(), user.getUsername());
      log.info("Trial period expired for user: {}", user.getId());
    }
  }

  @Transactional(readOnly = true)
  public boolean hasAccess(UUID userId) {
    try {
      UserEntity userEntity =
          userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

      return userEntity.getUserStatus() == UserStatus.ACTIVE
          && (userEntity.getTariffEndDate() == null
              || userEntity.getTariffEndDate().isAfter(LocalDateTime.now()));
    } catch (UserNotFoundException e) {
      log.error("User not found while checking access: {}", userId);
      return false;
    }
  }
}

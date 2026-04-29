package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.payment.dto.PaymentHistoryResponse;
import com.mipt.team4.cloud_storage_backend.model.payment.dto.PaymentTransactionDto;
import com.mipt.team4.cloud_storage_backend.model.payment.entity.PaymentTransaction;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.*;
import com.mipt.team4.cloud_storage_backend.model.user.dto.responses.TariffInfoResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TariffService {

  private static final long FREE_STORAGE_LIMIT = 5L * 1024 * 1024 * 1024;
  private static final int TRIAL_DAYS = 30;
  private static final int GRACE_PERIOD_DAYS = 30;

  private final UserJpaRepositoryAdapter userRepository;
  private final NotificationClient notificationClient;
  private final PaymentService paymentService;
  private final FileCleanupService fileCleanupService;
  private final PaymentTransactionRepository paymentTransactionRepository;

  @Transactional
  public void updatePaymentMethod(UpdatePaymentMethodRequest request) {
    UUID userId = request.userId();
    userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    userRepository.updatePaymentMethod(userId, request.paymentMethodId());
    log.info("Payment method updated for user: {}", userId);
  }

  @Transactional
  public void setAutoRenew(SetAutoRenewRequest request) {
    UUID userId = request.userId();
    userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    userRepository.updateAutoRenew(userId, request.enabled());
    log.info("Auto-renew {} for user: {}", request.enabled() ? "enabled" : "disabled", userId);
  }

  @Transactional(readOnly = true)
  public TariffInfoResponse getTariffInfo(TariffInfoRequest request) {
    UserEntity user =
        userRepository
            .getUserById(request.userId())
            .orElseThrow(() -> new UserNotFoundException(request.userId()));

    int daysLeft = 0;
    if (user.getTariffEndDate() != null && user.getTariffEndDate().isAfter(LocalDateTime.now())) {
      daysLeft = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), user.getTariffEndDate());
    }

    return new TariffInfoResponse(
        user.getTariffPlan(),
        user.getTotalStorageLimit(),
        user.getUsedStorage(),
        user.getFreeStorageLimit(),
        user.getTariffStartDate(),
        user.getTariffEndDate(),
        user.isAutoRenew(),
        user.getUserStatus() == UserStatus.ACTIVE,
        daysLeft,
        user.getTariffPlan() == null
            && user.getTrialEndDate() != null
            && user.getTrialEndDate().isAfter(LocalDateTime.now()),
        user.getTrialEndDate());
  }

  @Transactional
  public void purchaseTariff(PurchaseTariffRequest request) {
    UUID userId = request.userId();
    UserEntity user =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    TariffPlan plan = request.plan();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endDate = now.plusDays(plan.getDurationDays());
    BigDecimal amount = BigDecimal.valueOf(plan.getPriceRub());
    String paymentToken =
        request.paymentToken() != null ? request.paymentToken() : "test_" + UUID.randomUUID();

    PaymentTransaction transaction =
        createPendingTransaction(userId, plan, amount, request, paymentToken);

    try {
      paymentService.processPayment(userId, plan, paymentToken);

      transaction.complete();
      paymentTransactionRepository.save(transaction);

      userRepository.updateTariff(
          userId, plan, now, endDate, request.autoRenew(), plan.getStorageLimit());

      if (user.getTrialEndDate() != null) {
        userRepository.updateTrialDates(userId, null, null);
      }

      if (request.paymentMethod() != null) {
        userRepository.updatePaymentMethod(userId, request.paymentMethod());
      }

      if (user.getUserStatus() != UserStatus.ACTIVE) {
        userRepository.updateUserStatus(userId, UserStatus.ACTIVE);
        userRepository.updateScheduledDeletionDate(userId, null);
      }

      notificationClient.notifyTariffPurchased(
          user.getEmail(), user.getUsername(), plan.name(), endDate);
      log.info("User {} purchased tariff: {}", userId, plan.name());

    } catch (Exception e) {
      transaction.fail();
      paymentTransactionRepository.save(transaction);
      log.error("Payment failed for user: {}, plan: {}", userId, plan.name(), e);
      throw new RuntimeException("Payment failed: " + e.getMessage(), e);
    }
  }

  @Transactional
  public void setupTrialPeriod(UUID userId) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime trialEndDate = now.plusDays(TRIAL_DAYS);
    userRepository.updateTrialDates(userId, now, trialEndDate);
    log.info("Trial period started for user: {}, ends at: {}", userId, trialEndDate);
  }

  @Transactional(readOnly = true)
  public PaymentHistoryResponse getPaymentHistory(UUID userId) {
    userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    List<PaymentTransaction> transactions =
        paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    return PaymentHistoryResponse.of(
        transactions.stream().map(PaymentTransactionDto::from).toList());
  }

  @Transactional(readOnly = true)
  public boolean hasAccess(UUID userId) {
    return userRepository
        .getUserById(userId)
        .map(
            user ->
                user.getUserStatus() == UserStatus.ACTIVE
                    && (user.getTariffEndDate() == null
                        || user.getTariffEndDate().isAfter(LocalDateTime.now())))
        .orElse(false);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleAutoRenew(UserEntity user) {
    log.info("Auto-renewing for user: {}", user.getId());
    try {
      paymentService.processAutoRenewal(user.getId());
      LocalDateTime newEndDate =
          LocalDateTime.now().plusDays(user.getTariffPlan().getDurationDays());
      userRepository.updateTariffEndDate(user.getId(), newEndDate);
      notificationClient.notifyTariffRenewed(
          user.getEmail(), user.getUsername(), user.getTariffPlan().name(), newEndDate);
    } catch (Exception e) {
      log.error("Auto-renew failed for user: {}", user.getId(), e);
      restrictUserAccess(user);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void restrictUserAccess(UserEntity user) {
    userRepository.updateUserStatus(user.getId(), UserStatus.RESTRICTED);
    LocalDateTime deletionDate = LocalDateTime.now().plusDays(GRACE_PERIOD_DAYS);
    userRepository.updateScheduledDeletionDate(user.getId(), deletionDate);
    notificationClient.notifySubscriptionExpired(user.getEmail(), user.getUsername(), deletionDate);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkUsersInGracePeriod(UserEntity user) {
    long filesToDeleteSize = user.getUsedStorage() - FREE_STORAGE_LIMIT;
    if (filesToDeleteSize > 0) {
      fileCleanupService.deleteOldestFiles(user.getId(), filesToDeleteSize);
    }
    userRepository.updateTariff(user.getId(), null, null, null, false, null);
    userRepository.updateUserStatus(user.getId(), UserStatus.ACTIVE);
    userRepository.updateScheduledDeletionDate(user.getId(), null);
    notificationClient.notifyFilesDeleted(user.getEmail(), user.getUsername());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkExpiredTrials(UserEntity user) {
    userRepository.updateTrialDates(user.getId(), null, null);
    notificationClient.notifyTrialExpired(user.getEmail(), user.getUsername());
  }

  private PaymentTransaction createPendingTransaction(
      UUID userId, TariffPlan plan, BigDecimal amount, PurchaseTariffRequest req, String token) {
    return paymentTransactionRepository.save(
        PaymentTransaction.builder()
            .userId(userId)
            .tariffPlan(plan.name())
            .amount(amount)
            .paymentToken(token)
            .status("PENDING")
            .paymentMethod(req.paymentMethod())
            .autoRenew(req.autoRenew())
            .storageLimitGb(plan.getStorageLimit() / (1024 * 1024 * 1024)) // TODO: в константу
            .price(amount)
            .durationDays(plan.getDurationDays())
            .build());
  }
}

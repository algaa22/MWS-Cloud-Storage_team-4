package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TariffInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.PurchaseTariffRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SetAutoRenewRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.TariffInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdatePaymentMethodRequest;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TariffService {

  private final UserJpaRepositoryAdapter userRepository;
  private final NotificationClient notificationClient;
  private final PaymentService paymentService;

  @Transactional
  public void setupTrialPeriod(UUID userId) {
    userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endDate = now.plusDays(TariffPlan.TRIAL.getDurationDays());

    userRepository.updateTariff(
        userId, TariffPlan.TRIAL, now, endDate, false, TariffPlan.TRIAL.getStorageLimit());

    log.info("Trial period started for user: {}, ends at: {}", userId, endDate);
  }

  @Transactional
  public void purchaseTariff(PurchaseTariffRequest request) {
    UUID userId = request.userId();
    UserEntity userEntity =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    TariffPlan plan = request.plan();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endDate = now.plusDays(plan.getDurationDays());

    paymentService.processPayment(userId, plan, request.paymentToken());
    userRepository.updateTariff(
        userId, plan, now, endDate, request.autoRenew(), plan.getStorageLimit());

    if (request.paymentMethod() != null) {
      userRepository.updatePaymentMethod(userId, request.paymentMethod());
    }

    notificationClient.notifyTariffPurchased(
        userEntity.getEmail(), userEntity.getUsername(), plan.name(), endDate);

    log.info("User {} purchased tariff: {}", userId, plan.name());
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

    TariffPlan tariffPlan =
        userEntity.getTariffPlan() != null ? userEntity.getTariffPlan() : TariffPlan.TRIAL;
    LocalDateTime endDate = userEntity.getTariffEndDate();

    int daysLeft = 0;
    if (endDate != null && endDate.isAfter(LocalDateTime.now())) {
      daysLeft = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), endDate);
    }

    return new TariffInfoDto(
        tariffPlan,
        userEntity.getStorageLimit(),
        userEntity.getUsedStorage(),
        userEntity.getTariffStartDate(),
        endDate,
        userEntity.isAutoRenew(),
        userEntity.isActive(),
        daysLeft);
  }

  @Transactional(readOnly = true)
  public boolean hasAccess(UUID userId) {
    UserEntity userEntity =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    if (!userEntity.isActive()) {
      return false;
    }

    return userEntity.getTariffEndDate() == null
        || !userEntity.getTariffEndDate().isBefore(LocalDateTime.now());
  }
}

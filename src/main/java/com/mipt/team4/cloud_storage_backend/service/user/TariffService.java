package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.PaymentException;
import com.mipt.team4.cloud_storage_backend.exception.user.TariffPurchaseException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TariffInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.PurchaseTariffRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SimpleUserRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdateAutoRenewRequest;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TariffService {

  private final UserRepository userRepository;
  private final UserSessionService userSessionService;
  private final NotificationClient notificationClient;
  private final PaymentService paymentService;

  public void setupTrialPeriod(UUID userId) {
    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException(userId);
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endDate = now.plusDays(TariffPlan.TRIAL.getDurationDays());

    userRepository.updateTariff(
        userId, TariffPlan.TRIAL, now, endDate, false, TariffPlan.TRIAL.getStorageLimit());

    log.info("Trial period started for user: {}, ends at: {}", userId, endDate);
  }

  public void purchaseTariff(PurchaseTariffRequest request) {
    String token = request.userToken();
    UUID userId = userSessionService.extractUserIdFromToken(token);

    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException(token);
    }

    UserEntity user = userOpt.get();

    try {
      paymentService.processPayment(userId, request.tariffPlan(), request.paymentToken());

      LocalDateTime now = LocalDateTime.now();
      LocalDateTime endDate = now.plusDays(request.tariffPlan().getDurationDays());

      userRepository.updateTariff(
          userId,
          request.tariffPlan(),
          now,
          endDate,
          request.autoRenew(),
          request.tariffPlan().getStorageLimit());

      notificationClient.notifyTariffPurchased(
          user.getEmail(), user.getName(), request.tariffPlan().name(), endDate);

      log.info("User {} purchased tariff: {}", userId, request.tariffPlan());

    } catch (Exception e) {
      log.error("Payment failed for user: {}", userId, e);
      throw e;
    }
  }

  public TariffInfoDto getTariffInfo(SimpleUserRequest request) {
    String token = request.token();
    UUID userId = userSessionService.extractUserIdFromToken(token);

    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException("User not found with token: " + token);
    }

    UserEntity user = userOpt.get();
    int daysLeft = user.getTariffEndDate() != null ? (int) java.time.Duration.between(LocalDateTime.now(), user.getTariffEndDate()).toDays() : 0;
    
    return new TariffInfoDto(
        user.getTariffPlan() != null ? user.getTariffPlan() : TariffPlan.TRIAL,
        user.getStorageLimit(),
        user.getUsedStorage(),
        user.getTariffStartDate(),
        user.getTariffEndDate(),
        user.isAutoRenew(),
        user.isActive(),
        daysLeft);
  }

  public void disableAutoRenew(SimpleUserRequest request) {
    String token = request.token();
    UUID userId = userSessionService.extractUserIdFromToken(token);

    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException("User not found with token: " + token);
    }

    userRepository.updateAutoRenew(userId, false);
    log.info("Auto-renew disabled for user: {}", userId);
  }

  public void enableAutoRenew(SimpleUserRequest request) {
    String token = request.token();
    UUID userId = userSessionService.extractUserIdFromToken(token);

    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException("User not found with token: " + token);
    }

    userRepository.updateAutoRenew(userId, true);
    log.info("Auto-renew enabled for user: {}", userId);
  }

  public void updatePaymentMethod(UpdateAutoRenewRequest request) {
    String token = request.userToken();
    UUID userId = userSessionService.extractUserIdFromToken(token);

    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException("User not found with token: " + token);
    }

    userRepository.updatePaymentMethod(userId, request.paymentMethodId());
    log.info("Payment method updated for user: {}", userId);
  }

  public boolean hasAccess(SimpleUserRequest request) {
    String token = request.token();
    UUID userId = userSessionService.extractUserIdFromToken(token);
    return hasAccess(userId);
  }
  
  public boolean hasAccess(UUID userId) {
    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      return false;
    }

    UserEntity user = userOpt.get();

    if (!user.isActive()) {
      return false;
    }

    if (user.getTariffEndDate() != null && user.getTariffEndDate().isBefore(LocalDateTime.now())) {
      return false;
    }

    return true;
  }
}

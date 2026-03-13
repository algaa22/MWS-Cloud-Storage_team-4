package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.PaymentException;
import com.mipt.team4.cloud_storage_backend.exception.user.TariffPurchaseException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TariffInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SimpleUserRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.TariffRequest;
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

  /** Установить пробный период при регистрации */
  public void setupTrialPeriod(UUID userId) {
    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException("User not found with id: " + userId);
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endDate = now.plusDays(TariffPlan.TRIAL.getDurationDays());

    userRepository.updateTariff(
        userId, TariffPlan.TRIAL, now, endDate, false, TariffPlan.TRIAL.getStorageLimit());

    log.info("Trial period started for user: {}, ends at: {}", userId, endDate);
  }

  /** Купить тариф */
  public void purchaseTariff(TariffRequest request) {
    // ИСПРАВЛЕНО: для record используем userToken() вместо getUserToken()
    String token = request.userToken();
    UUID userId = userSessionService.extractUserIdFromToken(token);

    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException("User not found with token: " + token);
    }

    UserEntity user = userOpt.get();

    try {
      // ИСПРАВЛЕНО: tariffPlan() вместо getTariffPlan()
      // ИСПРАВЛЕНО: paymentToken() вместо getPaymentToken()
      paymentService.processPayment(userId, request.tariffPlan(), request.paymentToken());

      LocalDateTime now = LocalDateTime.now();
      // ИСПРАВЛЕНО: tariffPlan().getDurationDays()
      LocalDateTime endDate = now.plusDays(request.tariffPlan().getDurationDays());

      // ИСПРАВЛЕНО: все вызовы без "get"
      userRepository.updateTariff(
          userId,
          request.tariffPlan(),
          now,
          endDate,
          request.autoRenew(), // ИСПРАВЛЕНО: autoRenew() вместо isAutoRenew()
          request.tariffPlan().getStorageLimit());

      notificationClient.notifyTariffPurchased(
          user.getEmail(), user.getName(), request.tariffPlan().name(), endDate);

      log.info("User {} purchased tariff: {}", userId, request.tariffPlan());

    } catch (PaymentException e) {
      log.error("Payment failed for user: {}", userId, e);
      throw new TariffPurchaseException(
          "Payment processing failed for tariff: " + request.tariffPlan(), e);
    }
  }

  /** Получить информацию о текущем тарифе */
  public TariffInfoDto getTariffInfo(SimpleUserRequest request) {
    String token = request.token();
    UUID userId = userSessionService.extractUserIdFromToken(token);

    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException("User not found with token: " + token);
    }

    UserEntity user = userOpt.get();

    return new TariffInfoDto(
        user.getTariffPlan() != null ? user.getTariffPlan() : TariffPlan.TRIAL,
        user.getStorageLimit(),
        user.getUsedStorage(),
        user.getTariffStartDate(),
        user.getTariffEndDate(),
        user.isAutoRenew(),
        user.isActive());
  }

  /** Отключить автопродление */
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

  /** Включить автопродление */
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

  /** Обновить способ оплаты */
  public void updatePaymentMethod(UpdateAutoRenewRequest request) {
    // ИСПРАВЛЕНО: для record используем userToken() вместо getUserToken()
    String token = request.userToken();
    UUID userId = userSessionService.extractUserIdFromToken(token);

    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException("User not found with token: " + token);
    }

    // ИСПРАВЛЕНО: paymentMethodId() вместо getPaymentMethodId()
    userRepository.updatePaymentMethod(userId, request.paymentMethodId());
    log.info("Payment method updated for user: {}", userId);
  }

  /** Проверить доступ пользователя к файлам (для внутреннего использования) */
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

  /** Проверить доступ пользователя к файлам (для API) */
  public boolean hasAccess(SimpleUserRequest request) {
    String token = request.token();
    UUID userId = userSessionService.extractUserIdFromToken(token);
    return hasAccess(userId);
  }
}

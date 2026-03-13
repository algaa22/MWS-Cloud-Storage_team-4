package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.payment.InvalidPaymentTokenException;
import com.mipt.team4.cloud_storage_backend.exception.user.payment.PaymentMethodNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.tariff.IllegalTariffStateException;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final UserJpaRepositoryAdapter userRepository;

  public void processPayment(UUID userId, TariffPlan plan, String paymentToken) {
    log.info("Processing payment for user {}: plan={}, token={}", userId, plan, paymentToken);

    if (paymentToken == null || paymentToken.isBlank()) {
      throw new InvalidPaymentTokenException(paymentToken);
    }

    log.info("Payment successful for user: {}", userId);
  }

  public void autoRenewTariff(UUID userId) {
    log.info("Starting auto-renew for user: {}", userId);

    UserEntity user =
        userRepository
            .getUserById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    if (!user.isAutoRenew()) {
      log.info("Auto-renew is disabled for user: {}", userId);
      return;
    }

    String paymentMethodId = user.getPaymentMethodId();
    if (paymentMethodId == null || paymentMethodId.isBlank()) {
      throw new PaymentMethodNotFoundException(userId);
    }

    TariffPlan currentPlan = user.getTariffPlan();
    if (currentPlan == null || currentPlan == TariffPlan.TRIAL) {
      throw new IllegalTariffStateException(userId, currentPlan);
    }

    // TODO: заменить на реальный платежный шлюз)
    log.info(
        "Charging user {} for tariff {} using payment method: {}",
        userId,
        currentPlan,
        paymentMethodId);
    //      if (Math.random() < 0.1) {
    //        throw new PaymentException("Payment gateway error: insufficient funds",
    // HttpResponseStatus.INTERNAL_SERVER_ERROR);
    //      }

    log.info(
        "Auto-renew successful for user: {}, plan: {}, payment method: {}",
        userId,
        currentPlan,
        paymentMethodId);
  }
}

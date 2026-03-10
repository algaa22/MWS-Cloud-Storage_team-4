package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.PaymentException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final UserRepository userRepository;

  public void processPayment(UUID userId, TariffPlan plan, String paymentToken) {
    // TODO: интеграция с реальной платежной системой
    log.info("Processing payment for user {}: plan={}, token={}", userId, plan, paymentToken);

    if (paymentToken == null || paymentToken.isBlank()) {
      throw new PaymentException("Invalid payment token");
    }
    log.info("Payment successful for user: {}", userId);
  }

  public void autoRenewTariff(UUID userId) {
    log.info("Starting auto-renew for user: {}", userId);
    UserEntity user = userRepository.getUserById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    if (!user.isAutoRenew()) {
      log.info("Auto-renew is disabled for user: {}", userId);
      return;
    }
    String paymentMethodId = user.getPaymentMethodId();
    if (paymentMethodId == null || paymentMethodId.isBlank()) {
      log.error("No payment method saved for user: {}", userId);
      throw new PaymentException("No payment method saved for user: " + userId);
    }

    TariffPlan currentPlan = user.getTariffPlan();
    if (currentPlan == null || currentPlan == TariffPlan.TRIAL) {
      log.error("User {} has no valid tariff plan for auto-renew", userId);
      throw new PaymentException("No valid tariff plan for auto-renew");
    }

    try {
      //TODO: заменить на реальный платежный шлюз)
      log.info("Charging user {} for tariff {} using payment method: {}",
              userId, currentPlan, paymentMethodId);
      if (Math.random() < 0.1) {
        throw new PaymentException("Payment gateway error: insufficient funds");
      }

      log.info("Auto-renew successful for user: {}, plan: {}, payment method: {}",
              userId, currentPlan, paymentMethodId);

    } catch (Exception e) {
      log.error("Auto-renew failed for user: {}", userId, e);
      throw new PaymentException("Auto-renew failed: " + e.getMessage(), e);
    }
  }
}
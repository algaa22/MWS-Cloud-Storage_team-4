package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.payment.InvalidPaymentTokenException;
import com.mipt.team4.cloud_storage_backend.exception.user.payment.PaymentException;
import com.mipt.team4.cloud_storage_backend.exception.user.payment.PaymentMethodNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.tariff.IllegalTariffStateException;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final UserJpaRepositoryAdapter userRepository;

  @Transactional
  public void processPayment(UUID userId, TariffPlan plan, String paymentToken) {
    log.info("Processing payment for user {}: plan={}, token={}", userId, plan, paymentToken);

    if (paymentToken == null || paymentToken.isBlank()) {
      throw new InvalidPaymentTokenException("Payment token is required");
    }

    try {
      boolean paymentSuccessful = simulatePaymentGatewayCall(userId, plan, paymentToken);

      if (!paymentSuccessful) {
        throw new PaymentException(
            "Payment failed: insufficient funds or invalid payment method",
            HttpResponseStatus.PAYMENT_REQUIRED);
      }

      log.info("Payment successful for user: {}", userId);
    } catch (PaymentException e) {
      throw e;
    } catch (Exception e) {
      log.error("Payment processing failed for user: {}", userId, e);
      throw new PaymentException(
          "Payment processing failed: " + e.getMessage(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Transactional
  public void processAutoRenewal(UUID userId) {
    UserEntity userEntity =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    if (!userEntity.isAutoRenew()) {
      log.info("Auto-renew is disabled for user: {}", userId);
      return;
    }

    String paymentMethodId = userEntity.getPaymentMethodId();
    TariffPlan plan = userEntity.getTariffPlan();

    if (paymentMethodId == null || paymentMethodId.isBlank()) {
      throw new PaymentMethodNotFoundException(userId);
    }

    if (plan == null) {
      throw new IllegalTariffStateException(userId, null);
    }

    log.info("Processing auto-renewal for user: {}, plan: {}", userId, plan);

    try {
      boolean paymentSuccessful = simulateAutoRenewalCall(userId, plan, paymentMethodId);

      if (!paymentSuccessful) {
        throw new PaymentException(
            "Auto-renewal failed: payment method declined", HttpResponseStatus.PAYMENT_REQUIRED);
      }

      log.info(
          "Auto-renewal successful for user: {}, plan: {}, payment method: {}",
          userId,
          plan,
          paymentMethodId);
    } catch (PaymentException e) {
      throw e;
    } catch (Exception e) {
      log.error("Auto-renewal failed for user: {}", userId, e);
      throw new PaymentException(
          "Auto-renewal failed: " + e.getMessage(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private boolean simulatePaymentGatewayCall(UUID userId, TariffPlan plan, String paymentToken) {
    log.debug(
        "Simulating payment gateway call for user: {}, amount: {} rub", userId, plan.getPriceRub());

    return true;
  }

  private boolean simulateAutoRenewalCall(UUID userId, TariffPlan plan, String paymentMethodId) {
    log.debug(
        "Simulating auto-renewal call for user: {}, amount: {} rub", userId, plan.getPriceRub());

    return true;
  }
}

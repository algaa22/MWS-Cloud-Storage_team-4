package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.PaymentException;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    public void processPayment(UUID userId, TariffPlan plan, String paymentToken) {
        // TODO: интеграция с реальной платежной системой
        log.info("Processing payment for user {}: plan={}, token={}", userId, plan, paymentToken);

        if (paymentToken == null || paymentToken.isBlank()) {
            throw new PaymentException("Invalid payment token");
        }
    }

    public void autoRenewTariff(UUID userId) {
        // TODO: автоматическое списание
        log.info("Auto-renewing tariff for user: {}", userId);

        if (Math.random() < 0.1) {
            throw new PaymentException("Insufficient funds");
        }
    }
}
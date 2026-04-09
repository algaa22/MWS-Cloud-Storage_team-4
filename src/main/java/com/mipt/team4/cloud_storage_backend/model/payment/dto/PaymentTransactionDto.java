package com.mipt.team4.cloud_storage_backend.model.payment.dto;

import com.mipt.team4.cloud_storage_backend.model.payment.entity.PaymentTransaction;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionDto(
    @ResponseBodyParam String id,
    @ResponseBodyParam String tariffPlan,
    @ResponseBodyParam BigDecimal amount,
    @ResponseBodyParam String paymentToken,
    @ResponseBodyParam String status,
    @ResponseBodyParam String paymentMethod,
    @ResponseBodyParam LocalDateTime createdAt,
    @ResponseBodyParam LocalDateTime completedAt,
    @ResponseBodyParam Boolean autoRenew,
    @ResponseBodyParam Long storageLimitGb,
    @ResponseBodyParam BigDecimal price,
    @ResponseBodyParam Integer durationDays
    ) {
  public static PaymentTransactionDto from(PaymentTransaction transaction) {
    return new PaymentTransactionDto(
        transaction.getId().toString(),
        transaction.getTariffPlan(),
        transaction.getAmount(),
        transaction.getPaymentToken(),
        transaction.getStatus(),
        transaction.getPaymentMethod(),
        transaction.getCreatedAt(),
        transaction.getCompletedAt(),
        transaction.getAutoRenew(),
        transaction.getStorageLimitGb(),
        transaction.getPrice(),
        transaction.getDurationDays());
  }
}

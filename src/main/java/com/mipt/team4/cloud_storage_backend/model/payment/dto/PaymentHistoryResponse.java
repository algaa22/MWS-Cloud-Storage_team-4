package com.mipt.team4.cloud_storage_backend.model.payment.dto;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.util.List;

public record PaymentHistoryResponse(@ResponseBodyParam List<PaymentTransactionDto> transactions) {
  public static PaymentHistoryResponse of(List<PaymentTransactionDto> transactions) {
    return new PaymentHistoryResponse(transactions);
  }
}

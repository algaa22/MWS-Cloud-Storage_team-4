package com.mipt.team4.cloud_storage_backend.exception.user.payment;

import io.netty.handler.codec.http.HttpResponseStatus;

public class InsufficientFundsException extends PaymentException {
  public InsufficientFundsException(String paymentMethodId) {
    super("Insufficient funds on card: " + paymentMethodId, HttpResponseStatus.PAYMENT_REQUIRED);
  }
}

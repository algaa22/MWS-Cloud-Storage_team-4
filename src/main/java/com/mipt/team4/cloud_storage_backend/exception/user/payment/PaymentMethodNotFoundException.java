package com.mipt.team4.cloud_storage_backend.exception.user.payment;

import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.UUID;

public class PaymentMethodNotFoundException extends PaymentException {
  public PaymentMethodNotFoundException(UUID userId) {
    super("No payment method saved for user: " + userId, HttpResponseStatus.NOT_FOUND);
  }
}

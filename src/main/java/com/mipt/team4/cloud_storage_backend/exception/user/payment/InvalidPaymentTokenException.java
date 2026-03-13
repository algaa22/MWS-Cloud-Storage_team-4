package com.mipt.team4.cloud_storage_backend.exception.user.payment;

import io.netty.handler.codec.http.HttpResponseStatus;

public class InvalidPaymentTokenException extends PaymentException {
  public InvalidPaymentTokenException(String token) {
    super("Invalid payment token: " + token, HttpResponseStatus.BAD_REQUEST);
  }
}

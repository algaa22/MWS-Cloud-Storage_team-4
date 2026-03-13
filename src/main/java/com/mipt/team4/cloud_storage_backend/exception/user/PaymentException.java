package com.mipt.team4.cloud_storage_backend.exception.user;

public class PaymentException extends RuntimeException {

  public PaymentException(String message) {
    super(message);
  }

  public PaymentException(String message, Throwable cause) {
    super(message, cause);
  }
}

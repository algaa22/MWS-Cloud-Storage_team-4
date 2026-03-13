package com.mipt.team4.cloud_storage_backend.exception.user;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class PaymentException extends BaseStorageException {

  private static final HttpResponseStatus DEFAULT_STATUS = HttpResponseStatus.PAYMENT_REQUIRED;

  public PaymentException(String message) {
    super(message, DEFAULT_STATUS);
  }

  public PaymentException(String message, Throwable cause) {
    super(message, cause, DEFAULT_STATUS);
  }
}

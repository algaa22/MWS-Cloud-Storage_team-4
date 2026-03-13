package com.mipt.team4.cloud_storage_backend.exception.user.payment;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class PaymentException extends BaseStorageException {
  public PaymentException(String message, HttpResponseStatus status) {
    super(message, status);
  }
}

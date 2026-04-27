package com.mipt.team4.cloud_storage_backend.exception.user.tariff;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class TariffAccessDeniedException extends BaseStorageException {

  public TariffAccessDeniedException() {
    super(
        "Your tariff has expired. Please renew to continue.", HttpResponseStatus.PAYMENT_REQUIRED);
  }
}

package com.mipt.team4.cloud_storage_backend.exception.user.tariff;

import com.mipt.team4.cloud_storage_backend.exception.user.payment.PaymentException;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.UUID;

public class IllegalTariffStateException extends PaymentException {
  public IllegalTariffStateException(UUID userId, TariffPlan tariffPlan) {
    super(
        String.format(
            "User %s cannot auto-renew tariff: %s is not a renewable plan",
            userId, tariffPlan == null ? "NULL" : tariffPlan.name()),
        HttpResponseStatus.BAD_REQUEST);
  }
}

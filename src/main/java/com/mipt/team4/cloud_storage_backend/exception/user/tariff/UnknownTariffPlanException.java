package com.mipt.team4.cloud_storage_backend.exception.user.tariff;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Arrays;

public class UnknownTariffPlanException extends BaseStorageException {
  public UnknownTariffPlanException() {
    super(
        "Unknown tariff plan. Possible values: " + Arrays.toString(TariffPlan.values()),
        HttpResponseStatus.BAD_REQUEST);
  }
}

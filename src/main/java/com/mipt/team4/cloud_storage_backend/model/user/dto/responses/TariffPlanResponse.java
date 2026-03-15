package com.mipt.team4.cloud_storage_backend.model.user.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.ResponseBodyParam;

public record TariffPlanResponse(
    @ResponseBodyParam String name,
    @ResponseBodyParam long storageLimit,
    @ResponseBodyParam int priceRub,
    @ResponseBodyParam int durationDays) {
  public static TariffPlanResponse from(TariffPlan plan) {
    return new TariffPlanResponse(
        plan.name(), plan.getStorageLimit(), plan.getPriceRub(), plan.getDurationDays());
  }
}

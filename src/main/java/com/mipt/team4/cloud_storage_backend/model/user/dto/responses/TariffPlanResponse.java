package com.mipt.team4.cloud_storage_backend.model.user.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;

public record TariffPlanResponse(
    @ResponseBodyParam("name") String name,
    @ResponseBodyParam("storageLimit") long storageLimit,
    @ResponseBodyParam("priceRub") int priceRub,
    @ResponseBodyParam("durationDays") int durationDays) {
  public static TariffPlanResponse from(TariffPlan plan) {
    return new TariffPlanResponse(
        plan.name(), plan.getStorageLimit(), plan.getPriceRub(), plan.getDurationDays());
  }
}

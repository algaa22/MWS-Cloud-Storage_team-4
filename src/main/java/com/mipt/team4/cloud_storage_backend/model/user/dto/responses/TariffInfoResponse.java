package com.mipt.team4.cloud_storage_backend.model.user.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.time.LocalDateTime;

public record TariffInfoResponse(
    @ResponseBodyParam TariffPlan activeTariff,
    @ResponseBodyParam long totalStorageLimit,
    @ResponseBodyParam long usedStorage,
    @ResponseBodyParam long freeStorageLimit,
    @ResponseBodyParam LocalDateTime tariffStartDate,
    @ResponseBodyParam LocalDateTime tariffEndDate,
    @ResponseBodyParam boolean autoRenew,
    @ResponseBodyParam boolean isActive,
    @ResponseBodyParam int daysLeft,
    @ResponseBodyParam boolean hasActiveTrial,
    @ResponseBodyParam LocalDateTime trialEndDate) {}

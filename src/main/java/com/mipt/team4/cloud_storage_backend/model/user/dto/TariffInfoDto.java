package com.mipt.team4.cloud_storage_backend.model.user.dto;

import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;

import java.time.LocalDateTime;

public record TariffInfoDto(
    @ResponseBodyParam TariffPlan tariffPlan,
    @ResponseBodyParam long storageLimit,
    @ResponseBodyParam long usedStorage,
    @ResponseBodyParam LocalDateTime startDate,
    @ResponseBodyParam LocalDateTime endDate,
    @ResponseBodyParam boolean autoRenew,
    @ResponseBodyParam boolean isActive,
    @ResponseBodyParam int daysLeft) {}

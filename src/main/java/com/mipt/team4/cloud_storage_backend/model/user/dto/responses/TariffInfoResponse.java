package com.mipt.team4.cloud_storage_backend.model.user.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import java.time.LocalDateTime;

public record TariffInfoResponse(
    TariffPlan tariffPlan,
    long storageLimit,
    long usedStorage,
    LocalDateTime startDate,
    LocalDateTime endDate,
    boolean autoRenew,
    boolean isActive,
    int daysLeft) {}

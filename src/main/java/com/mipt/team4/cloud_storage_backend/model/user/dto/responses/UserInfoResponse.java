package com.mipt.team4.cloud_storage_backend.model.user.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.model.user.enums.UserStatus;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserInfoResponse(
    @ResponseBodyParam UUID id,
    @ResponseBodyParam String username,
    @ResponseBodyParam String email,
    @ResponseBodyParam UserStatus userStatus,
    @ResponseBodyParam long totalStorageLimit,
    @ResponseBodyParam long freeStorageLimit,
    @ResponseBodyParam long usedStorage,
    @ResponseBodyParam boolean isActive,
    @ResponseBodyParam TariffPlan activeTariff,
    @ResponseBodyParam LocalDateTime tariffStartDate,
    @ResponseBodyParam LocalDateTime tariffEndDate,
    @ResponseBodyParam boolean autoRenew,
    @ResponseBodyParam String paymentMethodId,
    @ResponseBodyParam boolean hasActiveTrial,
    @ResponseBodyParam LocalDateTime trialStartDate,
    @ResponseBodyParam LocalDateTime trialEndDate,
    @ResponseBodyParam LocalDateTime scheduledDeletionDate) {}

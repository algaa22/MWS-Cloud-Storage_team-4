package com.mipt.team4.cloud_storage_backend.model.user.dto.responses;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;

public record UserInfoResponse(
    @ResponseBodyParam String name,
    @ResponseBodyParam String email,
    @ResponseBodyParam long storageLimit,
    @ResponseBodyParam long usedStorage,
    @ResponseBodyParam boolean isActive) {}

package com.mipt.team4.cloud_storage_backend.model.user.dto.responses;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserInfoResponse(
    @ResponseBodyParam UUID id,
    @ResponseBodyParam String name,
    @ResponseBodyParam String email,
    @ResponseBodyParam String password,
    @ResponseBodyParam long storageLimit,
    @ResponseBodyParam long usedStorage,
    @ResponseBodyParam LocalDateTime createdAt,
    @ResponseBodyParam boolean isActive) {}

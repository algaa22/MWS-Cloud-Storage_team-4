package com.mipt.team4.cloud_storage_backend.model.user.dto.responses;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserInfoResponse(
    UUID id,
    String name,
    String email,
    String password,
    long storageLimit,
    long usedStorage,
    LocalDateTime createdAt,
    boolean isActive) {}

package com.mipt.team4.cloud_storage_backend.model.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
    UUID id,
    String name,
    String email,
    String password,
    String phoneNumber,
    long storageLimit,
    long used_storage,
    LocalDateTime createdAt,
    boolean isActive) {}

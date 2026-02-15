package com.mipt.team4.cloud_storage_backend.model.user.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefreshTokenEntity(
    UUID id, UUID userId, String token, LocalDateTime expiresAt, boolean revoked) {}

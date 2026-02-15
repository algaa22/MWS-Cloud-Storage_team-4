package com.mipt.team4.cloud_storage_backend.model.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefreshTokenDto(
    UUID id, UUID userId, String token, LocalDateTime expiresAt, boolean revoked) {}

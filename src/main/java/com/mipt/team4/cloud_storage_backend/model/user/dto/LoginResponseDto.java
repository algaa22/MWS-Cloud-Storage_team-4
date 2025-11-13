package com.mipt.team4.cloud_storage_backend.model.user.dto;

import java.util.UUID;

public record LoginResponseDto(
    String token,
    UUID userId,
    String email
) {}


package com.mipt.team4.cloud_storage_backend.model.user.dto;

import java.util.UUID;

public record UserSessionDto(UUID userId, String email, String token, long createdAt) {}

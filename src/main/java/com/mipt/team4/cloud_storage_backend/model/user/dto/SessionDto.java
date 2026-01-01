package com.mipt.team4.cloud_storage_backend.model.user.dto;

import java.util.UUID;

public record SessionDto(UUID userId, String email, String token, long createdAt) {

}

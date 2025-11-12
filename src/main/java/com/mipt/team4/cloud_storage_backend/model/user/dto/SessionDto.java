package com.mipt.team4.cloud_storage_backend.model.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionDto(
    UUID userId, String token, LocalDateTime createdAt, LocalDateTime expiredAt) {
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiredAt);
  }
}

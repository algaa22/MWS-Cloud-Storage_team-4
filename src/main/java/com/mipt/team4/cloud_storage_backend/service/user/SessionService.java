package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.SessionDto;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionService {
  private final Map<String, SessionDto> activeSessions = new ConcurrentHashMap<>();

  public void createSession(UUID userId, String token) {
    // TODO
  }

  public void isValidSession(UUID userId, String token) {
    // TODO: активна ли сессия
  }

  public void invalidateSession(UUID userId, String token) {
    // TODO: завершить сессию
  }
}

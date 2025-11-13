package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.SessionDto;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionService {
  // Ключ — токен (например, JWT), значение — сессия пользователя
  private final Map<String, SessionDto> activeSessions = new ConcurrentHashMap<>();

  // Создание новой сессии
  public void createSession(UUID userId, String token) {
    SessionDto session = new SessionDto(userId, token, System.currentTimeMillis());
    activeSessions.put(token, session);
  }

  // Проверка, что сессия (токен) действительна для данного пользователя
  public boolean isValidSession(UUID userId, String token) {
    SessionDto session = activeSessions.get(token);
    return session != null && session.userId().equals(userId);
  }

  // Завершить сессию (logout) — удалить по токену
  public void invalidateSession(UUID userId, String token) {
    SessionDto session = activeSessions.get(token);
    if (session != null && session.userId().equals(userId)) {
      activeSessions.remove(token);
    }
  }
}


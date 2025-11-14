package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.SessionDto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionService {

  private final Map<String, SessionDto> activeSessions = new ConcurrentHashMap<>();
  private final Map<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();

  public void createSession(UUID userId, String token) {
    SessionDto session = new SessionDto(userId, token, System.currentTimeMillis());
    activeSessions.put(token, session);
  }

  public boolean isValidSession(UUID userId, String token) {
    SessionDto session = activeSessions.get(token);
    return session != null && session.userId().equals(userId);
  }

  public void blacklistToken(String token, LocalDateTime expirationTime) {
    blacklistedTokens.put(token, expirationTime);
  }

  public boolean isBlacklisted(String token) {
    LocalDateTime expiry = blacklistedTokens.get(token);
    if (expiry == null) return false;

    if (LocalDateTime.now().isAfter(expiry)) {
      blacklistedTokens.remove(token); // очистка мусора
      return false;
    }
    return true;
  }
}

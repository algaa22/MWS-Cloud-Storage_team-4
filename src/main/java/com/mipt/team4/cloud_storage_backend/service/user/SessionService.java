package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.SessionDto;

import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionService {

  private final Map<String, SessionDto> activeSessions = new ConcurrentHashMap<>();
  private final Map<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();
  private final JwtService jwtService; // сервис для работы с JWT

  public SessionService(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  public SessionDto createSession(UserEntity user) {
    String token = jwtService.generateToken(user);
    SessionDto session =
        new SessionDto(user.getId(), user.getEmail(), token, System.currentTimeMillis());
    activeSessions.put(token, session);

    return session;
  }

  public boolean tokenExists(String token) {
    return activeSessions.containsKey(token);
  }

  public boolean isValidSession(UUID userId, String token) {
    // TODO: возможно надо
    SessionDto session = activeSessions.get(token);
    return session != null && session.userId().equals(userId);
  }

  public void blacklistToken(String token) {
    Optional<SessionDto> session = getSession(token);

    if (session.isEmpty()) {
      // TODO: exception
    }
    // TODO: clean blacklist and sessions
    blacklistedTokens.put(token, jwtService.getTokenExpiredDateTime());
  }

  public Optional<SessionDto> getSession(String token) {
    return Optional.ofNullable(activeSessions.get(token));
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

  public Optional<SessionDto> findSessionByEmail(String email) {
    for (Map.Entry<String, SessionDto> entry : activeSessions.entrySet()) {
      SessionDto session = entry.getValue();

      if (session.email().equals(email))
        return Optional.of(session);
    }

    return Optional.empty();
  }
}

package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.SessionDto;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class UserSessionService {

  // TODO: Сделать его синглтоном (?)
  private final Map<String, SessionDto> activeSessions = new ConcurrentHashMap<>();
  private final Map<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();
  private final Duration accessTokenDuration = Duration.ofMinutes(15);

  private final JwtService jwtService;

  public UserSessionService(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  public SessionDto createSession(UserEntity user) {
    String token = jwtService.generateAccessToken(user);
    SessionDto session =
        new SessionDto(user.getId(), user.getEmail(), token, System.currentTimeMillis());
    activeSessions.put(token, session);
    return session;
  }

  public boolean isSessionExpired(String token) {
    Optional<SessionDto> session = getSession(token);
    if (session.isEmpty()) {
      return true;
    }
    long sessionAge = System.currentTimeMillis() - session.get().createdAt();
    return sessionAge > accessTokenDuration.toMillis();
  }

  public boolean tokenExists(String token) {
    return activeSessions.containsKey(token);
  }

  public void blacklistToken(String token) throws InvalidSessionException {
    Optional<SessionDto> session = getSession(token);

    if (session.isEmpty()) {
      throw new InvalidSessionException("No session with specified token was found");
    }

    cleanExpiredBlacklistedTokens();
    blacklistedTokens.put(token, jwtService.getAccessTokenExpiredDateTime());
    // session.remove(token);
  }

  private void cleanExpiredBlacklistedTokens() {
    LocalDateTime now = LocalDateTime.now();
    Iterator<Entry<String, LocalDateTime>> iterator = blacklistedTokens.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, LocalDateTime> entry = iterator.next();
      if (entry.getValue().isBefore(now)) {
        iterator.remove();
      }
    }
  }

  public Optional<SessionDto> getSession(String token) {
    return Optional.ofNullable(activeSessions.get(token));
  }

  public boolean isBlacklisted(String token) {
    LocalDateTime expiry = blacklistedTokens.get(token);
    if (expiry == null) {
      return false;
    }
    if (LocalDateTime.now().isAfter(expiry)) {
      blacklistedTokens.remove(token);
      return false;
    }
    return true;
  }

  public Optional<SessionDto> findSessionByEmail(String email) {
    for (Map.Entry<String, SessionDto> entry : activeSessions.entrySet()) {
      SessionDto session = entry.getValue();
      if (session.email().equals(email)) {
        return Optional.of(session);
      }
    }
    return Optional.empty();
  }

  public UUID extractUserIdFromToken(String token) throws UserNotFoundException {
    if (isBlacklisted(token) || isSessionExpired(token)) {
      throw new UserNotFoundException(token);
    }

    Optional<SessionDto> userSession = getSession(token);
    if (userSession.isEmpty()) {
      throw new UserNotFoundException(token);
    }

    return userSession.get().userId();
  }

  public void revokeAllUserSessions(UUID userId) {
    // TODO: O(n)
    activeSessions.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId));
  }
}

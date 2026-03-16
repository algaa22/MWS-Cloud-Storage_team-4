package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserSessionDto;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Менеджер активных пользовательских сессий и механизмов отзыва токенов.
 *
 * <p>Данная реализация использует In-Memory хранилище (ConcurrentHashMap). Для обеспечения
 * безопасности при выходе (logout) используется механизм <b>Blacklisting</b>: токены помечаются как
 * недействительные до момента их естественного истечения.
 */
@Service
@RequiredArgsConstructor
public class UserSessionService {
  // TODO: scheduler для очищение истекших access и blacklisted токенов
  private final AccessTokenService accessTokenService;

  private final Map<UUID, Set<String>> userTokensIndex = new ConcurrentHashMap<>();
  private final Map<String, UserSessionDto> activeSessions = new ConcurrentHashMap<>();
  private final Map<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();

  public UserSessionDto createSession(UserEntity user) {
    String token = accessTokenService.generateAccessToken(user);
    UserSessionDto session =
        new UserSessionDto(user.getId(), user.getEmail(), token, System.currentTimeMillis());

    activeSessions.put(token, session);
    userTokensIndex.computeIfAbsent(user.getId(), k -> ConcurrentHashMap.newKeySet()).add(token);

    return session;
  }

  public Optional<UserSessionDto> getSession(String token) {
    if (!isTokenValid(token)) {
      return Optional.empty();
    }

    return Optional.ofNullable(activeSessions.get(token));
  }

  public boolean isTokenValid(String token) {
    return accessTokenService.isValid(token)
        && activeSessions.containsKey(token)
        && !isBlacklisted(token);
  }

  /**
   * Добавляет токен в черный список и инициирует превентивную очистку.
   *
   * <p>Токен будет считаться невалидным, даже если его подпись верна и срок действия еще не истек.
   * Очистка черного списка происходит <b>On-Demand</b> при каждом вызове метода для экономии
   * памяти.
   */
  public void blacklistToken(String token) {
    UserSessionDto session = activeSessions.remove(token);
    if (session == null) {
      throw new InvalidSessionException();
    }

    Set<String> tokens = userTokensIndex.get(session.userId());
    if (tokens != null) {
      tokens.remove(token);
    }

    LocalDateTime expiry = accessTokenService.getAccessTokenExpiredDateTime();
    blacklistedTokens.put(token, expiry);

    cleanExpiredBlacklistedTokens();
  }

  public void revokeAllUserSessions(UUID userId) {
    Set<String> tokens = userTokensIndex.remove(userId);

    if (tokens != null) {
      for (String token : tokens) {
        activeSessions.remove(token);
      }
    }
  }

  private void cleanExpiredBlacklistedTokens() {
    LocalDateTime now = LocalDateTime.now();
    blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
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

  public Optional<UserSessionDto> findSessionByEmail(String email) {
    for (Map.Entry<String, UserSessionDto> entry : activeSessions.entrySet()) {
      UserSessionDto session = entry.getValue();
      if (session.email().equals(email)) {
        return Optional.of(session);
      }
    }
    return Optional.empty();
  }
}

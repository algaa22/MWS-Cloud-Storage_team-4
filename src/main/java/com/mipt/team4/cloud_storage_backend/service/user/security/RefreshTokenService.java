package com.mipt.team4.cloud_storage_backend.service.user.security;

import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import com.mipt.team4.cloud_storage_backend.repository.user.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Сервис управления долгоживущими Refresh-токенами.
 *
 * <p>Использует стратегию <b>Opaque Tokens</b>: токены представляют собой криптографически стойкие
 * случайные строки, состояние которых (срок действия, признак отзыва) хранится в базе данных. Это
 * позволяет мгновенно аннулировать сессии пользователей (например, при логауте или смене пароля).
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
  private final RefreshTokenRepository repository;
  private final SecureRandom random;

  public RefreshTokenDto create(UUID userId) {
    RefreshTokenDto token =
        new RefreshTokenDto(
            UUID.randomUUID(), userId, generateToken(), LocalDateTime.now().plusDays(30), false);

    repository.save(token);
    return token;
  }

  public RefreshTokenDto validate(String token) {
    return repository
        .findByToken(token)
        .filter(t -> !t.revoked())
        .filter(t -> t.expiresAt().isAfter(LocalDateTime.now()))
        .orElse(null);
  }

  public void revoke(String token) {
    repository.findByToken(token).ifPresent(t -> repository.revokeById(t.id()));
  }

  public void revokeAllForUser(UUID userId) {
    repository.deleteByUserId(userId);
  }

  private String generateToken() {
    byte[] bytes = new byte[64];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}

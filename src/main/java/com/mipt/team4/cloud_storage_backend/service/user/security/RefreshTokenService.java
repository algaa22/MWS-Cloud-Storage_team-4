package com.mipt.team4.cloud_storage_backend.service.user.security;

import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import com.mipt.team4.cloud_storage_backend.repository.user.RefreshTokenRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository repository;
  private final SecureRandom random;

  private String generateToken() {
    byte[] bytes = new byte[64];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

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
}

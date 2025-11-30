package com.mipt.team4.cloud_storage_backend.service.user.security;

import com.mipt.team4.cloud_storage_backend.model.user.entity.RefreshTokenEntity;
import com.mipt.team4.cloud_storage_backend.repository.user.RefreshTokenRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

public class RefreshTokenService {

  private final RefreshTokenRepository repository;
  private final SecureRandom random = new SecureRandom();

  public RefreshTokenService(RefreshTokenRepository repository) {
    this.repository = repository;
  }

  private String generateToken() {
    byte[] bytes = new byte[64];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public RefreshTokenEntity create(UUID userId) {
    RefreshTokenEntity token = new RefreshTokenEntity(
        UUID.randomUUID(),
        userId,
        generateToken(),
        LocalDateTime.now().plusDays(30),
        false
    );

    repository.save(token);
    return token;
  }

  public RefreshTokenEntity validate(String token) {
    return repository.findByToken(token)
        .filter(t -> !t.isRevoked())
        .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
        .orElse(null);
  }

  public void revoke(String token) {
    repository.findByToken(token)
        .ifPresent(t -> repository.revokeById(t.getId()));
  }

  public void revokeAllForUser(UUID userId) {
    repository.deleteByUserId(userId);
  }
}


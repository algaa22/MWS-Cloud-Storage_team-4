package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.model.refreshtoken.entity.RefreshTokenEntity;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

  private final RefreshTokenJpaRepository jpaRepository;

  @Transactional
  public void revokeById(UUID id) {
    jpaRepository.revokeById(id);
  }

  @Transactional
  public void deleteByUserId(UUID userId) {
    jpaRepository.deleteByUserId(userId);
  }

  @Transactional
  public void save(RefreshTokenDto dto) {
    RefreshTokenEntity entity =
        RefreshTokenEntity.builder()
            .id(dto.id())
            .userId(dto.userId())
            .token(dto.token())
            .expiresAt(dto.expiresAt())
            .revoked(dto.revoked())
            .build();

    jpaRepository.save(entity);
  }

  @Transactional(readOnly = true)
  public Optional<RefreshTokenDto> findByToken(String tokenStr) {
    return jpaRepository
        .findByToken(tokenStr)
        .map(
            entity ->
                new RefreshTokenDto(
                    entity.getId(),
                    entity.getUserId(),
                    entity.getToken(),
                    entity.getExpiresAt(),
                    entity.isRevoked()));
  }
}

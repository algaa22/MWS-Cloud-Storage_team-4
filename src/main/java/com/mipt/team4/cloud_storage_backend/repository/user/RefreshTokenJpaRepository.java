package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.model.refreshtoken.entity.RefreshTokenEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {
  @Modifying(flushAutomatically = true)
  void deleteByUserId(UUID userId);

  @Modifying(flushAutomatically = true)
  @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.id = :id")
  void revokeById(@Param("id") UUID id);

  Optional<RefreshTokenEntity> findByToken(String token);
}

package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByEmail(String email);

  @Modifying
  @Query(
      "UPDATE UserEntity u SET u.usedStorage = "
          + "CASE WHEN (u.usedStorage + :delta) < 0 THEN 0 ELSE (u.usedStorage + :delta) END "
          + "WHERE u.id = :id")
  void updateUsedStorage(@Param("id") UUID id, @Param("delta") long delta);

  @Modifying
  @Query(
      value =
          """
        UPDATE users u
        SET used_storage = COALESCE(
            (SELECT SUM(size)
             FROM files f
             WHERE f.user_id = u.id
               AND f.is_deleted = FALSE),
            0)
        """,
      nativeQuery = true)
  void syncAllUsersStorage();
}

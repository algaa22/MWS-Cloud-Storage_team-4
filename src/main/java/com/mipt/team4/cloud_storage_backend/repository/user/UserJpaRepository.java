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

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE UserEntity u SET u.usedStorage = " +
            "CASE WHEN (u.usedStorage + :delta) < 0 THEN 0 ELSE (u.usedStorage + :delta) END " +
            "WHERE u.id = :id")
    void updateUsedStorage(@Param("id") UUID id, @Param("delta") long delta);
}

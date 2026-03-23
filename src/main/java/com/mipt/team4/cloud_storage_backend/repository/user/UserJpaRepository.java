package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.model.storage.projection.StorageUsageProjection;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import java.time.LocalDateTime;
import java.util.List;
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

  @Modifying
  @Query("UPDATE UserEntity u SET u.autoRenew = :autoRenew WHERE u.id = :userId")
  void updateAutoRenew(@Param("userId") UUID userId, @Param("autoRenew") boolean autoRenew);

  @Modifying
  @Query(
      """
        UPDATE UserEntity u
        SET u.tariffPlan = :plan,
            u.tariffStartDate = :startDate,
            u.tariffEndDate = :endDate,
            u.autoRenew = :autoRenew,
            u.storageLimit = :storageLimit
        WHERE u.id = :userId
    """)
  void updateTariff(
      @Param("userId") UUID userId,
      @Param("plan") TariffPlan plan,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("autoRenew") boolean autoRenew,
      @Param("storageLimit") long storageLimit);

  @Modifying
  @Query("UPDATE UserEntity u SET u.tariffEndDate = :newEndDate WHERE u.id = :userId")
  void updateTariffEndDate(
      @Param("userId") UUID userId, @Param("newEndDate") LocalDateTime newEndDate);

  @Modifying
  @Query("UPDATE UserEntity u SET u.paymentMethodId = :paymentMethodId WHERE u.id = :userId")
  void updatePaymentMethod(
      @Param("userId") UUID userId, @Param("paymentMethodId") String paymentMethodId);

  @Modifying
  @Query("UPDATE UserEntity u SET u.isActive = :isActive WHERE u.id = :userId")
  void updateActiveStatus(@Param("userId") UUID userId, @Param("isActive") boolean isActive);

  @Query(
      "SELECT u.usedStorage as usedStorage, u.storageLimit as storageLimit FROM UserEntity u WHERE u.id = :userId")
  Optional<StorageUsageProjection> findStorageUsageById(@Param("userId") UUID userId);

  List<UserEntity> findAllByTariffEndDateBetweenAndIsActiveTrue(
      LocalDateTime from, LocalDateTime to);

  List<UserEntity> findAllByTariffEndDateBeforeAndIsActiveTrue(LocalDateTime now);

  List<UserEntity> findAllByTrialStartDateBetween(LocalDateTime start, LocalDateTime end);
}

package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.model.storage.projection.StorageUsageProjection;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.model.user.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByEmail(String email);

  @Modifying(flushAutomatically = true)
  @Query(
      "UPDATE UserEntity u SET u.usedStorage = "
          + "CASE WHEN (u.usedStorage + :delta) < 0 THEN 0 ELSE (u.usedStorage + :delta) END "
          + "WHERE u.id = :id")
  void updateUsedStorage(@Param("id") UUID id, @Param("delta") long delta);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
              UPDATE users u
              SET used_storage = s.total_size
              FROM (
                  SELECT user_id, COALESCE(SUM(size), 0) as total_size
                  FROM files
                  WHERE is_deleted = FALSE
                  GROUP BY user_id
              ) s
              WHERE u.id = s.user_id;
              """,
      nativeQuery = true)
  void syncAllUsersStorage();

  @Modifying(flushAutomatically = true)
  @Query("UPDATE UserEntity u SET u.autoRenew = :autoRenew WHERE u.id = :userId")
  void updateAutoRenew(@Param("userId") UUID userId, @Param("autoRenew") boolean autoRenew);

  @Modifying(flushAutomatically = true)
  @Query(
      """
          UPDATE UserEntity u
          SET u.tariffPlan = :plan,
              u.tariffStartDate = :startDate,
              u.tariffEndDate = :endDate,
              u.autoRenew = :autoRenew,
              u.paidStorageLimit = :storageLimit
          WHERE u.id = :userId
          """)
  void updateTariff(
      @Param("userId") UUID userId,
      @Param("plan") TariffPlan plan,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      @Param("autoRenew") boolean autoRenew,
      @Param("storageLimit") Long storageLimit);

  @Modifying(flushAutomatically = true)
  @Query("UPDATE UserEntity u SET u.tariffEndDate = :newEndDate WHERE u.id = :userId")
  void updateTariffEndDate(
      @Param("userId") UUID userId, @Param("newEndDate") LocalDateTime newEndDate);

  @Modifying(flushAutomatically = true)
  @Query("UPDATE UserEntity u SET u.paymentMethodId = :paymentMethodId WHERE u.id = :userId")
  void updatePaymentMethod(
      @Param("userId") UUID userId, @Param("paymentMethodId") String paymentMethodId);

  @Modifying(flushAutomatically = true)
  @Query("UPDATE UserEntity u SET u.isActive = :isActive WHERE u.id = :userId")
  void updateActiveStatus(@Param("userId") UUID userId, @Param("isActive") boolean isActive);

  @Modifying
  @Query("UPDATE UserEntity u SET u.userStatus = :status WHERE u.id = :userId")
  void updateUserStatus(@Param("userId") UUID userId, @Param("status") UserStatus status);

  @Modifying
  @Query("UPDATE UserEntity u SET u.scheduledDeletionDate = :deletionDate WHERE u.id = :userId")
  void updateScheduledDeletionDate(
      @Param("userId") UUID userId, @Param("deletionDate") LocalDateTime deletionDate);

  @Modifying
  @Query(
      "UPDATE UserEntity u SET u.trialStartDate = :startDate, u.trialEndDate = :endDate WHERE u.id = :userId")
  void updateTrialDates(
      @Param("userId") UUID userId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  @Query(
      "SELECT u.usedStorage as usedStorage, "
          + "u.freeStorageLimit as freeStorageLimit, "
          + "u.paidStorageLimit as paidStorageLimit "
          + "FROM UserEntity u WHERE u.id = :userId")
  Optional<StorageUsageProjection> findStorageUsageById(@Param("userId") UUID userId);

  @Query(
      "SELECT u FROM UserEntity u WHERE u.tariffEndDate BETWEEN :from AND :to AND u.userStatus = :status")
  List<UserEntity> findAllByTariffEndDateBetweenAndUserStatus(
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("status") UserStatus status);

  @Query("SELECT u FROM UserEntity u WHERE u.tariffEndDate < :now AND u.userStatus = :status")
  List<UserEntity> findAllByTariffEndDateBeforeAndUserStatus(
      @Param("now") LocalDateTime now, @Param("status") UserStatus status);

  @Query("SELECT u FROM UserEntity u WHERE u.tariffEndDate < :now AND u.userStatus = :status")
  Slice<UserEntity> findAllByTariffEndDateBeforeAndUserStatus(
      @Param("now") LocalDateTime now, @Param("status") UserStatus status, Pageable pageable);

  List<UserEntity> findAllByUserStatusAndScheduledDeletionDateBefore(
      @Param("status") UserStatus status, @Param("date") LocalDateTime date);

  @Query("SELECT u FROM UserEntity u WHERE u.trialEndDate < :now AND u.tariffPlan IS NULL")
  List<UserEntity> findAllByTrialEndDateBeforeAndTariffPlanIsNull(@Param("now") LocalDateTime now);

  List<UserEntity> findAllByTrialStartDateBetween(LocalDateTime start, LocalDateTime end);

  @Deprecated
  List<UserEntity> findAllByTariffEndDateBetweenAndIsActiveTrue(
      LocalDateTime from, LocalDateTime to);

  @Deprecated
  List<UserEntity> findAllByTariffEndDateBeforeAndIsActiveTrue(LocalDateTime now);

  @Query(
      "SELECT u FROM UserEntity u WHERE u.tariffEndDate BETWEEN :from AND :to AND u.userStatus = :status")
  Slice<UserEntity> findAllByTariffEndDateBetweenAndUserStatus(
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("status") UserStatus status,
      Pageable pageable);
}

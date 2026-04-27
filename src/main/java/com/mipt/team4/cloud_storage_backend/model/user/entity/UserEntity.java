package com.mipt.team4.cloud_storage_backend.model.user.entity;

import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.model.user.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor()
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserEntity {
  @Builder.Default
  @Column(name = "is_active")
  private boolean isActive = true;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "email", unique = true, nullable = false)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Builder.Default
  @Column(name = "free_storage_limit")
  private long freeStorageLimit = 5L * 1024 * 1024 * 1024; // 5GB постоянно

  @Column(name = "paid_storage_limit")
  private Long paidStorageLimit; // может быть null если нет платного тарифа

  @Builder.Default
  @Column(name = "used_storage")
  private long usedStorage = 0L;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "tariff_plan")
  private TariffPlan tariffPlan;

  @Column(name = "tariff_start_date")
  private LocalDateTime tariffStartDate;

  @Column(name = "tariff_end_date")
  private LocalDateTime tariffEndDate;

  @Builder.Default
  @Column(name = "auto_renew")
  private boolean autoRenew = true;

  @Column(name = "payment_method_id")
  private String paymentMethodId;

  @Column(name = "trial_start_date")
  private LocalDateTime trialStartDate;

  @Column(name = "trial_end_date")
  private LocalDateTime trialEndDate;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "user_status")
  private UserStatus userStatus = UserStatus.ACTIVE;

  @Column(name = "scheduled_deletion_date")
  private LocalDateTime scheduledDeletionDate;

  public long getTotalStorageLimit() {
    if (paidStorageLimit != null) {
      return freeStorageLimit + paidStorageLimit;
    }
    return freeStorageLimit;
  }

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}

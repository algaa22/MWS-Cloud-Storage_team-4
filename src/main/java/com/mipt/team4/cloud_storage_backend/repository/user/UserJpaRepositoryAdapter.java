package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageUsage;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.model.user.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UserJpaRepositoryAdapter {
  private final UserJpaRepository jpaRepository;

  public void addUser(UserEntity userEntity) {
    if (userEntity.getId() != null && jpaRepository.existsById(userEntity.getId())) {
      throw new UserAlreadyExistsException(userEntity.getId());
    }

    jpaRepository.saveAndFlush(userEntity);
  }

  @Transactional
  public void increaseUsedStorage(UUID id, long delta) {
    jpaRepository.updateUsedStorage(id, delta);
  }

  @Transactional
  public void decreaseUsedStorage(UUID id, long delta) {
    jpaRepository.updateUsedStorage(id, -delta);
  }

  @Transactional
  public void syncAllUsersStorage() {
    jpaRepository.syncAllUsersStorage();
  }

  @Transactional
  public void updateAutoRenew(UUID userId, boolean autoRenew) {
    jpaRepository.updateAutoRenew(userId, autoRenew);
  }

  @Transactional
  public void updateTariff(
      UUID userId,
      TariffPlan plan,
      LocalDateTime startDate,
      LocalDateTime endDate,
      boolean autoRenew,
      Long storageLimit) { // Изменено с long на Long для возможности null
    jpaRepository.updateTariff(userId, plan, startDate, endDate, autoRenew, storageLimit);
  }

  @Transactional
  public void updateTariffEndDate(UUID userId, LocalDateTime newEndDate) {
    jpaRepository.updateTariffEndDate(userId, newEndDate);
  }

  @Transactional
  public void updatePaymentMethod(UUID userId, String paymentMethodId) {
    jpaRepository.updatePaymentMethod(userId, paymentMethodId);
  }

  @Transactional
  public void updateUserStatus(UUID userId, UserStatus status) {
    jpaRepository.updateUserStatus(userId, status);
  }

  @Transactional
  public void updateScheduledDeletionDate(UUID userId, LocalDateTime deletionDate) {
    jpaRepository.updateScheduledDeletionDate(userId, deletionDate);
  }

  @Transactional
  public void updateTrialDates(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
    jpaRepository.updateTrialDates(userId, startDate, endDate);
  }

  @Transactional
  public void deactivateUser(UUID userId) {
    jpaRepository.updateActiveStatus(userId, false);
  }

  @Transactional
  public void activateUser(UUID userId) {
    jpaRepository.updateActiveStatus(userId, true);
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> getUserByEmail(String email) {
    return jpaRepository.findByEmail(email);
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> getUserById(UUID id) {
    return jpaRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<UserEntity> getUsersWithTariffEndingBetween(LocalDateTime from, LocalDateTime to) {
    return jpaRepository.findAllByTariffEndDateBetweenAndUserStatus(from, to, UserStatus.ACTIVE);
  }

  @Transactional(readOnly = true)
  public List<UserEntity> getUsersWithExpiredTariff(LocalDateTime now) {
    return jpaRepository.findAllByTariffEndDateBeforeAndUserStatus(now, UserStatus.ACTIVE);
  }

  @Transactional(readOnly = true)
  public List<UserEntity> getUsersWithTrialStartedToday() {
    LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
    LocalDateTime endOfDay = startOfDay.plusDays(1);
    return jpaRepository.findAllByTrialStartDateBetween(startOfDay, endOfDay);
  }

  @Transactional(readOnly = true)
  public List<UserEntity> findAllByUserStatusAndScheduledDeletionDateBefore(
      UserStatus status, LocalDateTime date) {
    return jpaRepository.findAllByUserStatusAndScheduledDeletionDateBefore(status, date);
  }

  @Transactional(readOnly = true)
  public List<UserEntity> findAllByTrialEndDateBeforeAndTariffPlanIsNull(LocalDateTime now) {
    return jpaRepository.findAllByTrialEndDateBeforeAndTariffPlanIsNull(now);
  }

  @Transactional(readOnly = true)
  public Optional<StorageUsage> getStorageUsage(UUID userId) {
    return jpaRepository
            .findStorageUsageById(userId)
            .map(projection -> {
              // Безопасное получение usedStorage
              long used = projection.getUsedStorage() != null ?
                      projection.getUsedStorage() : 0L;

              // Безопасное получение freeStorageLimit (по умолчанию 5GB)
              long freeLimit = projection.getFreeStorageLimit() != null ?
                      projection.getFreeStorageLimit() : 5L * 1024 * 1024 * 1024;

              // Безопасное получение paidStorageLimit (по умолчанию 0)
              long paidLimit = projection.getPaidStorageLimit() != null ?
                      projection.getPaidStorageLimit() : 0L;

              // Общий лимит = бесплатный + платный
              long totalLimit = freeLimit + paidLimit;

              return new StorageUsage(used, totalLimit);
            });
  }
}

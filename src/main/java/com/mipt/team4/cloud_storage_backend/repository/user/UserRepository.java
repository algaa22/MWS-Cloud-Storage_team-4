package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageUsage;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepository {
  private final PostgresConnection postgres;

  public void addUser(UserEntity userEntity) {

    if (userExists(userEntity.getId())) {
      throw new UserAlreadyExistsException(userEntity.getId());
    }

    postgres.executeUpdate(
        "INSERT INTO users (id, email, password_hash, username, storage_limit, used_storage, created_at, is_active)"
            + " values (?, ?, ?, ?, ?, ?, ?, ?);",
        List.of(
            userEntity.getId(),
            userEntity.getEmail(),
            userEntity.getPasswordHash(),
            userEntity.getName(),
            userEntity.getStorageLimit(),
            userEntity.getUsedStorage(),
            userEntity.getCreatedAt(),
            userEntity.isActive()));
  }

  public Optional<UserEntity> getUserByEmail(String email) {
    List<UserEntity> result;
    result =
        postgres.executeQuery(
            "SELECT * FROM users WHERE email = ?;",
            List.of(email),
            this::createUserEntityByResultSet);

    if (result.isEmpty()) {
      return Optional.empty();
    }

    return Optional.ofNullable(result.getFirst());
  }

  public Optional<UserEntity> getUserById(UUID id) {
    List<UserEntity> result =
        postgres.executeQuery(
            "SELECT * FROM users WHERE id = ?", List.of(id), this::createUserEntityByResultSet);

    if (result.isEmpty()) {
      return Optional.empty();
    }

    return Optional.ofNullable(result.getFirst());
  }

  public boolean userExists(UUID id) {
    List<Boolean> result =
        postgres.executeQuery(
            "SELECT EXISTS (SELECT 1 FROM users WHERE id = ?);",
            List.of(id),
            rs -> (rs.getBoolean(1)));
    return result.getFirst();
  }

  public void updateInfo(UUID id, String newName, String newPasswordHash) {
    List<String> updates = new ArrayList<>();
    List<Object> params = new ArrayList<>();

    updates.add("username = ?");
    updates.add("password_hash = ?");

    params.add(newName);
    params.add(newPasswordHash);
    params.add(id);

    String sql = String.format("UPDATE users SET %s WHERE id = ?;", String.join(", ", updates));

    postgres.executeUpdate(sql, params);
  }

  public void increaseUsedStorage(UUID id, long delta) {
    changeUsedStorage(id, delta);
  }

  public void decreaseUsedStorage(UUID id, long delta) {
    changeUsedStorage(id, -delta);
  }

  private void changeUsedStorage(UUID id, long delta) {
    postgres.executeUpdate(
        "UPDATE users SET used_storage = GREATEST(0, used_storage + ?) WHERE id = ?;",
        List.of(delta, id));
  }

  private UserEntity createUserEntityByResultSet(ResultSet rs) throws SQLException {
    return UserEntity.builder()
        .id(UUID.fromString(rs.getString("id")))
        .name(rs.getString("username"))
        .email(rs.getString("email"))
        .passwordHash(rs.getString("password_hash"))
        .storageLimit(rs.getLong("storage_limit"))
        .usedStorage(rs.getLong("used_storage"))
        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
        .isActive(rs.getBoolean("is_active"))
        .tariffPlan(getTariffPlan(rs, "tariff_plan"))
        .tariffStartDate(getLocalDateTime(rs, "tariff_start_date"))
        .tariffEndDate(getLocalDateTime(rs, "tariff_end_date"))
        .autoRenew(rs.getBoolean("auto_renew"))
        .paymentMethodId(rs.getString("payment_method_id"))
        .trialStartDate(getLocalDateTime(rs, "trial_start_date"))
        .build();
  }

  public Optional<StorageUsage> getStorageUsage(UUID userId) {
    String sql = "SELECT used_storage, storage_limit FROM users WHERE id = ?";
    List<StorageUsage> result =
        postgres.executeQuery(
            sql,
            List.of(userId),
            rs -> new StorageUsage(rs.getLong("used_storage"), rs.getLong("storage_limit")));
    return result.stream().findFirst();
  }

  public void updateTariff(
      UUID userId,
      TariffPlan plan,
      LocalDateTime startDate,
      LocalDateTime endDate,
      boolean autoRenew,
      long storageLimit) {
    postgres.executeUpdate(
        "UPDATE users SET tariff_plan = ?, tariff_start_date = ?, tariff_end_date = ?, "
            + "auto_renew = ?, storage_limit = ? WHERE id = ?;",
        List.of(
            plan.name(),
            startDate != null ? Timestamp.valueOf(startDate) : null,
            endDate != null ? Timestamp.valueOf(endDate) : null,
            autoRenew,
            storageLimit,
            userId));
  }

  public void updateTariffEndDate(UUID userId, LocalDateTime newEndDate) {
    postgres.executeUpdate(
        "UPDATE users SET tariff_end_date = ? WHERE id = ?;",
        List.of(Timestamp.valueOf(newEndDate), userId));
  }

  public void updateAutoRenew(UUID userId, boolean autoRenew) {
    postgres.executeUpdate(
        "UPDATE users SET auto_renew = ? WHERE id = ?;", List.of(autoRenew, userId));
  }

  public void updatePaymentMethod(UUID userId, String paymentMethodId) {
    postgres.executeUpdate(
        "UPDATE users SET payment_method_id = ? WHERE id = ?;", List.of(paymentMethodId, userId));
  }

  public void deactivateUser(UUID userId) {
    postgres.executeUpdate("UPDATE users SET is_active = false WHERE id = ?;", List.of(userId));
  }

  public void activateUser(UUID userId) {
    postgres.executeUpdate("UPDATE users SET is_active = true WHERE id = ?;", List.of(userId));
  }

  public List<UserEntity> findUsersWithTariffEndingBetween(LocalDateTime from, LocalDateTime to) {
    String sql = "SELECT * FROM users WHERE tariff_end_date BETWEEN ? AND ? AND is_active = true";
    return postgres.executeQuery(
        sql,
        List.of(Timestamp.valueOf(from), Timestamp.valueOf(to)),
        this::createUserEntityByResultSet);
  }

  public List<UserEntity> findUsersWithExpiredTariff(LocalDateTime now) {
    String sql = "SELECT * FROM users WHERE tariff_end_date < ? AND is_active = true";
    return postgres.executeQuery(
        sql, List.of(Timestamp.valueOf(now)), this::createUserEntityByResultSet);
  }

  public List<UserEntity> findUsersWithTrialStartedToday() {
    LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
    LocalDateTime endOfDay = startOfDay.plusDays(1);

    String sql = "SELECT * FROM users WHERE trial_start_date BETWEEN ? AND ?";
    return postgres.executeQuery(
        sql,
        List.of(Timestamp.valueOf(startOfDay), Timestamp.valueOf(endOfDay)),
        this::createUserEntityByResultSet);
  }

  private LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(column);
    return timestamp != null ? timestamp.toLocalDateTime() : null;
  }

  private TariffPlan getTariffPlan(ResultSet rs, String column) throws SQLException {
    String value = rs.getString(column);
    return value != null ? TariffPlan.valueOf(value) : TariffPlan.TRIAL;
  }
}

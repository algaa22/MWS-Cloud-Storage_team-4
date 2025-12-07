package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRepository {
  private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

  PostgresConnection postgres;

  public UserRepository(PostgresConnection postgres) {
    this.postgres = postgres;
  }

  public void addUser(UserEntity userEntity) throws UserAlreadyExistsException {

    if (userExists(userEntity.getId())) throw new UserAlreadyExistsException(userEntity.getId());

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
            rs ->
                new UserEntity(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password_hash"),
                    rs.getLong("storage_limit"),
                    rs.getLong("used_storage"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getBoolean("is_active")));

    if (result.isEmpty()) return Optional.empty();

    return Optional.ofNullable(result.getFirst());
  }

  public Optional<UserEntity> getUserById(UUID id) {
    List<UserEntity> result =
        postgres.executeQuery(
            "SELECT * FROM users WHERE id = ?",
            List.of(id),
            rs ->
                new UserEntity(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password_hash"),
                    rs.getLong("storage_limit"),
                    rs.getLong("used_storage"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getBoolean("is_active")));

    if (result.isEmpty()) return Optional.empty();

    return Optional.ofNullable(result.getFirst());
  }

  public boolean userExists(UUID id) {
    List<Boolean> result =
        postgres.executeQuery(
            "SELECT EXISTS (SELECT 1 FROM files WHERE id = ?);",
            List.of(id),
            rs -> (rs.getBoolean(1)));
    return result.getFirst();
  }

  public void updateInfo(UUID id, String newName, String newPasswordHash) {
    if (newName == null && newPasswordHash == null) {
      throw new IllegalArgumentException("At least one parameter must be provided");
    }

    List<String> updates = new ArrayList<>();
    List<Object> params = new ArrayList<>();

    if (newName != null) {
      updates.add("username = ?");
      params.add(newName);
    }

    if (newPasswordHash != null) {
      updates.add("password_hash = ?");
      params.add(newPasswordHash); // Это уже хеш!
    }

    if (updates.isEmpty()) {
      return;
    }

    params.add(id);
    String sql = String.format("UPDATE users SET %s WHERE id = ?;", String.join(", ", updates));

    postgres.executeUpdate(sql, params);
  }
  }

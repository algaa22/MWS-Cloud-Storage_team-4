package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserRepository {
  private final PostgresConnection postgresConnection;

  public UserRepository(PostgresConnection postgresConnection) {
    this.postgresConnection = postgresConnection;
    this.postgresConnection.connect();
  }

  public Optional<UserEntity> findByEmail(String email) {
    List<UserEntity> result = postgresConnection.executeQuery(
        "SELECT id, email, password_hash, username, storage_limit, used_storage, is_active FROM users WHERE email = ?",
        List.of(email),
        rs -> new UserEntity(
            UUID.fromString(rs.getString("id")),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getLong("storage_limit"),
            rs.getLong("used_storage"),
            (LocalDateTime) rs.getObject("created_at"),
            rs.getBoolean("is_active")
        )
    );
    if (result.isEmpty()) return Optional.empty();
    return Optional.of(result.get(0));
  }

  public void saveUser(UserEntity entity) {
    postgresConnection.executeUpdate(
        "INSERT INTO users (id, email, password_hash, username, storage_limit, used_storage, created at, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        List.of(
            entity.getId(),
            entity.getEmail(),
            entity.getPassword(),
            entity.getName(),
            entity.getStorageLimit(),
            entity.getUsedStorage(),
            entity.isActive()
        )
    );
  }

  public Optional<UserEntity> findById(UUID id) {
    List<UserEntity> result = postgresConnection.executeQuery(
        "SELECT id, email, password_hash, username, storage_limit, used_storage, is_active FROM users WHERE id = ?",
        List.of(id),
        rs -> new UserEntity(
            UUID.fromString(rs.getString("id")),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getLong("storage_limit"),
            rs.getLong("used_storage"),
            (LocalDateTime) rs.getObject("created_at"),
            rs.getBoolean("is_active")
        )
    );
    if (result.isEmpty()) return Optional.empty();
    return Optional.of(result.get(0));
  }

  public void updateUser(UserEntity user) {
    postgresConnection.executeUpdate(
        "UPDATE users SET email = ?, password_hash = ?, username = ?, storage_limit = ?, used_storage = ?, is_active = ? WHERE id = ?",
        List.of(
            user.getEmail(),
            user.getPassword(),
            user.getName(),
            user.getStorageLimit(),
            user.getUsedStorage(),
            user.isActive(),
            user.getId()
        )
    );
  }
}

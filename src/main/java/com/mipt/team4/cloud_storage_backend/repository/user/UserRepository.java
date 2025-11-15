package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
            userEntity.getPassword(),
            userEntity.getName(),
            userEntity.getStorageLimit(),
            userEntity.getUsedStorage(),
            userEntity.getCreatedAt(),
            userEntity.isActive()));
  }

  public void deleteUser(UUID id) throws UserNotFoundException {
    if (!userExists(id)) {
      throw new UserNotFoundException(id);
    }

    postgres.executeUpdate("DELETE FROM users WHERE id = ?;", List.of(id));
  }

  public Optional<UserEntity> getUser(String email) {
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

  public boolean userExists(UUID id) {
    List<Boolean> result =
        postgres.executeQuery(
            "SELECT EXISTS (SELECT 1 FROM files WHERE id = ?);",
            List.of(id),
            rs -> (rs.getBoolean(1)));
    return result.getFirst();
  }

  public void updateUser(UserEntity user) {
    // TODO
  }
}

package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.model.user.entity.RefreshTokenEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RefreshTokenRepository {

  private final PostgresConnection postgres;

  public RefreshTokenRepository(PostgresConnection postgres) {
    this.postgres = postgres;
  }

  public void revokeById(UUID id) {
    postgres.executeUpdate("UPDATE refresh_tokens SET revoked = TRUE WHERE id = ?;", List.of(id));
  }

  public void deleteByUser(UUID userId) {
    postgres.executeUpdate("DELETE FROM refresh_tokens WHERE user_id = ?;", List.of(userId));
  }

  public void deleteByToken(String token) {
    postgres.executeUpdate("DELETE FROM refresh_tokens WHERE token = ?;", List.of(token));
  }

  public void save(RefreshTokenEntity token) {
    postgres.executeUpdate(
        "INSERT INTO refresh_tokens (id, user_id, token, expires_at, revoked) VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (id) DO UPDATE SET token = EXCLUDED.token, expires_at = EXCLUDED.expires_at, revoked = EXCLUDED.revoked;",
        List.of(token.getId(), token.getUserId(), token.getToken(), token.getExpiresAt(), token.isRevoked()));
  }

  public Optional<RefreshTokenEntity> findByToken(String tokenStr) {
    List<RefreshTokenEntity> result =
        postgres.executeQuery(
            "SELECT id, user_id, token, expires_at, revoked FROM refresh_tokens WHERE token = ?;",
            List.of(tokenStr),
            rs -> new RefreshTokenEntity(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getString("token"),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getBoolean("revoked")
            ));
    if (result.isEmpty()) return Optional.empty();
    return Optional.of(result.getFirst());
  }

  public Optional<RefreshTokenEntity> findByUserId(UUID userId) {
    List<RefreshTokenEntity> result =
        postgres.executeQuery(
            "SELECT id, user_id, token, expires_at, revoked FROM refresh_tokens WHERE user_id = ?;",
            List.of(userId),
            rs -> new RefreshTokenEntity(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getString("token"),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getBoolean("revoked")
            ));
    if (result.isEmpty()) return Optional.empty();
    return Optional.of(result.getFirst());
  }
}

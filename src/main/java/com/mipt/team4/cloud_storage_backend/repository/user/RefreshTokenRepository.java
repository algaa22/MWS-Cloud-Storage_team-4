package com.mipt.team4.cloud_storage_backend.repository.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

  private final PostgresConnection postgres;

  public void revokeById(UUID id) {
    postgres.executeUpdate("UPDATE refresh_tokens SET revoked = TRUE WHERE id = ?;", List.of(id));
  }

  public void deleteByUserId(UUID userId) {
    postgres.executeUpdate("DELETE FROM refresh_tokens WHERE user_id = ?;", List.of(userId));
  }

  public void save(RefreshTokenDto token) {
    postgres.executeUpdate(
        "INSERT INTO refresh_tokens (id, user_id, token, expires_at, revoked) VALUES (?, ?, ?, ?, ?) "
            + "ON CONFLICT (id) DO UPDATE SET token = EXCLUDED.token, expires_at = EXCLUDED.expires_at, revoked = EXCLUDED.revoked;",
        List.of(token.id(), token.userId(), token.token(), token.expiresAt(), token.revoked()));
  }

  public Optional<RefreshTokenDto> findByToken(String tokenStr) {
    List<RefreshTokenDto> result =
        postgres.executeQuery(
            "SELECT id, user_id, token, expires_at, revoked FROM refresh_tokens WHERE token = ?;",
            List.of(tokenStr),
            rs ->
                new RefreshTokenDto(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("user_id")),
                    rs.getString("token"),
                    rs.getTimestamp("expires_at").toLocalDateTime(),
                    rs.getBoolean("revoked")));
    if (result.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(result.getFirst());
  }
}

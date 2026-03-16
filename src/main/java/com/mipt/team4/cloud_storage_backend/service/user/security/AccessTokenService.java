package com.mipt.team4.cloud_storage_backend.service.user.security;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TokenClaimsDto;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Сервис управления жизненным циклом Access-токенов (JWT). *
 *
 * <p>Отвечает за генерацию и валидацию короткоживущих токенов доступа. Использует алгоритм
 * HMAC-SHA256 для обеспечения целостности данных.
 */
@Service
public class AccessTokenService {
  private final long accessTokenExpirationSec;
  private final Key signingKey;

  public AccessTokenService(StorageConfig storageConfig) {
    String jwtSecretKey = storageConfig.auth().jwtSecretKey();
    this.accessTokenExpirationSec = storageConfig.auth().accessTokenExpirationSec();
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey));
  }

  public String generateAccessToken(UserEntity user) {
    return generateToken(user, accessTokenExpirationSec);
  }

  public boolean isValid(String token) {
    return extractTokenClaims(token).isValid();
  }

  public TokenClaimsDto extractTokenClaims(String token) {
    try {
      Claims claims =
          Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();

      return new TokenClaimsDto(UUID.fromString(claims.getSubject()), true);
    } catch (JwtException | IllegalArgumentException e) {
      return new TokenClaimsDto(null, false);
    }
  }

  public LocalDateTime getAccessTokenExpiredDateTime() {
    return LocalDateTime.now().plusSeconds(accessTokenExpirationSec);
  }

  /**
   * Генерирует детерминированный токен с уникальным идентификатором (JTI).
   *
   * <p><b>ч:</b>
   *
   * <ul>
   *   <li>{@code sub}: ID пользователя
   *   <li>{@code jti}: UUID для уникальности каждой генерации
   *   <li>{@code tokenType}: Явное разделение на access/refresh
   * </ul>
   */
  private String generateToken(UserEntity user, long expirationSec) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationSec * 1000L);

    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("jti", UUID.randomUUID())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }
}

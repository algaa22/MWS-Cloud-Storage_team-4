package com.mipt.team4.cloud_storage_backend.service.user.security;

import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

public class JwtService {
  private final String jwtSecretKey;
  private final long jwtTokenExpirationSec;

  public JwtService(String jwtSecretKey, long jwtTokenExpirationSec) {
    this.jwtSecretKey = jwtSecretKey;
    this.jwtTokenExpirationSec = jwtTokenExpirationSec;
  }

  public String generateToken(UserEntity user) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtTokenExpirationSec);

    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", "USER") // если есть роль - добавляй здесь
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(
            Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey)),
            SignatureAlgorithm.HS256)
        .compact();
  }

  // Проверяет подпись и срок действия токена
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder()
          .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey)))
          .build()
          .parseClaimsJws(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  // Получает userId (subject) из токена
  public String getUserIdFromToken(String token) {
    Claims claims =
        Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();
    return claims.getSubject();
  }

  public LocalDateTime getTokenExpiredDateTime() {
    return LocalDateTime.now().plusSeconds(jwtTokenExpirationSec);
  }

}

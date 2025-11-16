package com.mipt.team4.cloud_storage_backend.service.user.security;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.LocalDateTime;
import java.util.Date;

public class JwtService {
  private final long jwtTokenExpirationSec;

  public JwtService(long jwtTokenExpirationSec) {
    this.jwtTokenExpirationSec = jwtTokenExpirationSec;
  }

  // Проверяет подпись и срок действия токена
  public static boolean isTokenValid(String token) {
    try {
      Jwts.parserBuilder()
          .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(getJwtSecretKey())))
          .build()
          .parseClaimsJws(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private static String getJwtSecretKey() {
    return StorageConfig.INSTANCE.getJwtSecretKey();
  }

//  public String getUserIdFromToken(String token) {
//    String jwtSecretKey = StorageConfig.INSTANCE.getJwtSecretKey();
//
//    Claims claims =
//        Jwts.parserBuilder()
//            .setSigningKey(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)))
//            .build()
//            .parseClaimsJws(token)
//            .getBody();
//
//    return claims.getSubject();
//  }

  public String generateToken(UserEntity user) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtTokenExpirationSec * 1000L);

    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", "USER") // TODO: если есть роль - добавляй здесь
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(
            Keys.hmacShaKeyFor(Decoders.BASE64.decode(getJwtSecretKey())),
            SignatureAlgorithm.HS256)
        .compact();
  }

  public LocalDateTime getTokenExpiredDateTime() {
    return LocalDateTime.now().plusSeconds(jwtTokenExpirationSec);
  }
}

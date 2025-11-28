package com.mipt.team4.cloud_storage_backend.service.user.security;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

public class JwtService {
  private final long accessTokenExpirationSec;
  private final long refreshTokenExpirationSec;

  public JwtService(long accessTokenExpirationSec, long refreshTokenExpirationSec) {
    this.accessTokenExpirationSec = accessTokenExpirationSec;
    this.refreshTokenExpirationSec = refreshTokenExpirationSec;
  }

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

  public String generateAccessToken(UserEntity user) {
    return generateToken(user, accessTokenExpirationSec);
  }

  public String generateRefreshToken(UserEntity user) {
    return generateToken(user, refreshTokenExpirationSec);
  }

  private String generateToken(UserEntity user, long expirationSec) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationSec * 1000L);

    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", "USER")
        .claim("tokenType", expirationSec == accessTokenExpirationSec ? "access" : "refresh")
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(
            Keys.hmacShaKeyFor(Decoders.BASE64.decode(getJwtSecretKey())),
            SignatureAlgorithm.HS256)
        .compact();
  }

  public LocalDateTime getAccessTokenExpiredDateTime() {
    return LocalDateTime.now().plusSeconds(accessTokenExpirationSec);
  }

  public LocalDateTime getRefreshTokenExpiredDateTime() {
    return LocalDateTime.now().plusSeconds(refreshTokenExpirationSec);
  }

  public String getUserIdFromToken(String token) {
    Claims claims =
        Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(getJwtSecretKey())))
            .build()
            .parseClaimsJws(token)
            .getBody();

    return claims.getSubject(); // userId
  }

  public boolean isAccessToken(String token) {
    try {
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(getJwtSecretKey())))
          .build()
          .parseClaimsJws(token)
          .getBody();

      String tokenType = claims.get("tokenType", String.class);
      return "access".equals(tokenType);
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public boolean isRefreshToken(String token) {
    try {
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(getJwtSecretKey())))
          .build()
          .parseClaimsJws(token)
          .getBody();

      String tokenType = claims.get("tokenType", String.class);
      return "refresh".equals(tokenType);
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
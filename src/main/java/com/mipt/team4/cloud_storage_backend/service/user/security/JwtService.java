package com.mipt.team4.cloud_storage_backend.service.user.security;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final StorageConfig storageConfig;

  private final long accessTokenExpirationSec;
  private final long refreshTokenExpirationSec;
  private final String jwtSecretKey;

  public JwtService(
      StorageConfig storageConfig) {
    this.storageConfig = storageConfig;
    
    this.jwtSecretKey = storageConfig.auth().jwtSecretKey();
    this.accessTokenExpirationSec = storageConfig.auth().accessTokenExpirationSec();
    this.refreshTokenExpirationSec = storageConfig.auth().refreshTokenExpirationSec();
  }

  public boolean isTokenValid(String token) {
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
        .claim("jti", UUID.randomUUID())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(
            Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey)), SignatureAlgorithm.HS256)
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
            .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey)))
            .build()
            .parseClaimsJws(token)
            .getBody();

    return claims.getSubject(); // userId
  }

  public boolean isAccessToken(String token) {
    try {
      Claims claims =
          Jwts.parserBuilder()
              .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey)))
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
      Claims claims =
          Jwts.parserBuilder()
              .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey)))
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

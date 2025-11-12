package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.RegisterRequestDto;

public class JwtService {
  // В StorageConfig хранится jwtSecretKey, jwtTokenExpirationMs,

  public String generateToken(RegisterRequestDto registerRequest) {
    // TODO: используй Jwts lib
    return null;
  }

  public boolean validateToken(String token) {
    // TODO: валиден ли токен (не подделан и не просрочен)
    return false;
  }

  public String getUserIdFromToken(String token) {
    // TODO
    return null;
  }
}
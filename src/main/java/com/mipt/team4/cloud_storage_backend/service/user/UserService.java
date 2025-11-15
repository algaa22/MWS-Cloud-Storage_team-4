package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.*;
import com.mipt.team4.cloud_storage_backend.model.user.UserMapper;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.*;

public class UserService {
  private final UserRepository userRepository;
  private final JwtService jwtService; // сервис для работы с JWT
  private final SessionService sessionService;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
    this.jwtService =
        new JwtService(
            StorageConfig.INSTANCE.getJwtSecretKey(),
            StorageConfig.INSTANCE.getJwtTokenExpirationSec());
    this.sessionService = new SessionService();
  }

  // Регистрация нового пользователя
  public void registerUser(RegisterRequestDto registerRequest) throws UserAlreadyExistsException {
    // 1. Проверяем, нет ли такого логина
    if (userRepository.findByEmail(registerRequest.email()).isPresent())
      throw new UserAlreadyExistsException(registerRequest.email());

    String hash = PasswordHasher.hash(registerRequest.password());
    UserEntity entity =
        new UserEntity(
            UUID.randomUUID(), registerRequest.userName(), registerRequest.email(), hash);

    userRepository.saveUser(entity);
  }

  // Логин: возвращает токен при успехе
  public LoginResponseDto loginUser(LoginRequestDto loginRequest)
      throws WrongPasswordException, InvalidEmailOrPassword {
    Optional<UserEntity> userOpt = userRepository.findByEmail(loginRequest.email());
    if (userOpt.isEmpty()) throw new InvalidEmailOrPassword();

    UserEntity user = userOpt.get();
    if (!PasswordHasher.verify(loginRequest.password(), user.getPassword())) {
      throw new WrongPasswordException();
    }

    String jwt = jwtService.generateToken(user);
    // TODO: Можно добавить refresh-токен и логику сессий по необходимости
    return new LoginResponseDto(jwt, user.getId(), user.getEmail());
  }

  // Логаут (обычно для refresh-токенов/сессий)
  public void logoutUser(LogoutRequestDto logoutRequest) {
    LocalDateTime expirationTime = jwtService.getTokenExpiredDateTime();
    sessionService.blacklistToken(logoutRequest.token(), expirationTime);
  }

  public UserDto updateUser(String userId, UserDto dto) throws UserNotFoundException {
    UserEntity user =
        userRepository
            .findById(UUID.fromString(userId))
            .orElseThrow(() -> new UserNotFoundException(dto.email()));
    // TODO: подумать, что еще можно будет обновлять
    user.setName(dto.name());
    userRepository.updateUser(user);
    return UserMapper.toDto(user);
  }
}

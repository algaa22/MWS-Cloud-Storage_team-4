package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.*;
import com.mipt.team4.cloud_storage_backend.model.user.UserMapper;
import com.mipt.team4.cloud_storage_backend.repository.storage.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import java.util.*;
import java.util.stream.Collectors;


public class UserService {
  private final UserRepository userRepository;
  private final JwtService jwtService;    // сервис для работы с JWT
  private final SessionService sessionService;

  public UserService(UserRepository userRepository, JwtService jwtService,
      SessionService sessionService) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.sessionService = sessionService;
  }

  // Регистрация нового пользователя
  public void registerUser(RegisterRequestDto registerRequest) {
    if (userRepository.findByEmail(registerRequest.email()).isPresent()) {
      throw new UserAlreadyExistsException("User already exists", registerRequest.email());
    }
    String hash = PasswordHasher.hash(registerRequest.password());
    UserEntity entity = new UserEntity(
        UUID.randomUUID(),
        registerRequest.userName(),
        registerRequest.email(),
        hash,
        registerRequest.phoneNumber()
    );
    userRepository.save(entity);
  }

  // Логин: возвращает токен при успехе
  public LoginResponseDto loginUser(LoginRequestDto loginRequest) {
    Optional<UserEntity> userOpt = userRepository.findByEmail(loginRequest.email());
    if (userOpt.isEmpty()) throw new InvalidEmailOrPassword("No such user");
    UserEntity user = userOpt.get();
    if (!PasswordHasher.verify(loginRequest.password(), user.getPassword())) {
      throw new WrongPasswordException("Password incorrect");
    }
    String jwt = jwtService.generateToken(user);
    //TODO: Добавляем refresh-токены?
    return new LoginResponseDto(jwt, user.getId(), user.getEmail());
  }

  // Логаут (обычно для refresh-токенов/сессий)
  public void logoutUser(LogoutRequestDto logoutRequest) {
    long expirationTime = jwtService.getExpiration(logoutRequest.token());
    sessionService.blacklistToken(logoutRequest.token(), expirationTime);
  }

  public UserDto getUser(UUID id) {
    return userRepository
        .findById(id)
        .map(UserMapper::toDto)
        .orElseThrow(() -> new RuntimeException("User not found"));
  }

  public List<UserDto> getAllUsers() {
    return userRepository.findAll()
        .stream()
        .map(UserMapper::toDto)
        .collect(Collectors.toList());
  }

  public void deleteUser(UUID id) {
    userRepository.deleteById(id); // soft- или hard-delete по архитектуре
  }

  public UserDto updateUser(UUID userId, UserDto dto) {
    UserEntity user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));
    // TODO: подумать, что еще можно будет обновлять
    user.setName(dto.name());
    userRepository.update(user);
    return UserMapper.toDto(user);
  }
}

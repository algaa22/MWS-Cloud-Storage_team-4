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
  private final SessionService sessionService;
  //TODO: getUserInfo
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
    this.sessionService =
        new SessionService(
            new JwtService(
                StorageConfig.INSTANCE.getJwtSecretKey(),
                StorageConfig.INSTANCE.getJwtTokenExpirationSec()));
  }

  // Регистрация нового пользователя
  public String registerUser(RegisterRequestDto registerRequest) throws UserAlreadyExistsException {
    // 1. Проверяем, нет ли такого логина
    if (userRepository.getUser(registerRequest.email()).isPresent())
      throw new UserAlreadyExistsException(registerRequest.email());

    String hash = PasswordHasher.hash(registerRequest.password());
    UserEntity userEntity =
        new UserEntity(
            UUID.randomUUID(), registerRequest.userName(), registerRequest.email(), hash);

    userRepository.addUser(userEntity);
    SessionDto session = sessionService.createSession(userEntity);

    return session.token();
  }

  public String loginUser(LoginRequestDto loginRequest)
      throws WrongPasswordException, InvalidEmailOrPassword {
    Optional<UserEntity> userOpt = userRepository.getUser(loginRequest.email());
    if (userOpt.isEmpty()) throw new InvalidEmailOrPassword();

    UserEntity user = userOpt.get();
    if (!PasswordHasher.verify(loginRequest.password(), user.getPassword())) {
      throw new WrongPasswordException();
    }

    Optional<SessionDto> session = sessionService.findSessionByEmail(user.getEmail());
    String token;

    if (session.isPresent()) token = session.get().token();
    else token = sessionService.createSession(user).token();

    // TODO: Можно добавить refresh-токен и логику сессий по необходимости
    return token;
  }

  // Логаут (обычно для refresh-токенов/сессий)
  public void logoutUser(LogoutRequestDto logoutRequest) throws UserNotFoundException {
    String token = logoutRequest.token();

    if (sessionService.tokenExists(token))
      sessionService.blacklistToken(token);
    else
      throw new UserNotFoundException(token);
  }

//  public UserDto updateUser(String email, UserDto dto) throws UserNotFoundException {
//    UserEntity user =
//        userRepository.getUser(email).orElseThrow(() -> new UserNotFoundException(dto.email()));
//    // TODO: подумать, что еще можно будет обновлять
//    user.setName(dto.name());
//    userRepository.updateUser(user);
//    return UserMapper.toDto(user);
//  }
}

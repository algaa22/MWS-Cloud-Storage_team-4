package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChangeFileMetadataDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.model.user.dto.*;
import com.mipt.team4.cloud_storage_backend.model.user.entity.RefreshTokenEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import com.mipt.team4.cloud_storage_backend.service.user.security.RefreshTokenService;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
import java.util.*;

public class UserService {
  private final UserRepository userRepository;
  private final UserSessionService userSessionService;
  private final RefreshTokenService refreshTokenService;

  public UserService(
      UserRepository userRepository,
      UserSessionService userSessionService,
      RefreshTokenService refreshTokenService) {
    this.userRepository = userRepository;
    this.userSessionService = userSessionService;
    this.refreshTokenService = refreshTokenService;
  }

  public UserEntity getUserInfo(String email) throws UserNotFoundException {
    Optional<UserEntity> userOpt = userRepository.getUserByEmail(email);

    if (userOpt.isEmpty()) {
      throw new UserNotFoundException(email);
    }
    UserEntity user = userOpt.get();
    return new UserEntity(
        null,
        user.getName(),
        user.getEmail(),
        null,
        user.getStorageLimit(),
        user.getUsedStorage(),
        null,
        user.isActive());
  }

  public String registerUser(RegisterRequestDto registerRequest) throws UserAlreadyExistsException {

    if (userRepository.getUserByEmail(registerRequest.email()).isPresent())
      throw new UserAlreadyExistsException(registerRequest.email());

    String hash = PasswordHasher.hash(registerRequest.password());
    UserEntity userEntity =
        new UserEntity(
            UUID.randomUUID(), registerRequest.userName(), registerRequest.email(), hash);

    userRepository.addUser(userEntity);

    SessionDto session = userSessionService.createSession(userEntity);

    return session.token();
  }

  public String loginUser(LoginRequestDto loginRequest)
      throws WrongPasswordException, InvalidEmailOrPassword {
    Optional<UserEntity> userOpt = userRepository.getUserByEmail(loginRequest.email());
    if (userOpt.isEmpty()) throw new InvalidEmailOrPassword();

    UserEntity user = userOpt.get();
    if (!PasswordHasher.verify(loginRequest.password(), user.getPassword())) {
      throw new WrongPasswordException();
    }

    Optional<SessionDto> session = userSessionService.findSessionByEmail(user.getEmail());
    SessionDto usedSession;
    if (session.isPresent()) {
      usedSession = session.get();
    } else {
      usedSession = userSessionService.createSession(user);
    }
    RefreshTokenEntity refreshEntity = refreshTokenService.create(user.getId());

    return usedSession.token();
  }

  public void logoutUser(LogoutRequestDto logoutRequest)
      throws UserNotFoundException, InvalidSessionException {
    String token = logoutRequest.token();

    if (userSessionService.tokenExists(token)) {
      userSessionService.blacklistToken(token);

      UUID userId = userSessionService.extractUserIdFromToken(token);
      refreshTokenService.revokeAllForUser(userId);
    } else {
      throw new UserNotFoundException(token);
    }
  }

  public String refreshTokens(String refreshToken) throws InvalidSessionException {
    RefreshTokenEntity stored = refreshTokenService.validate(refreshToken);
    if (stored == null) {
      throw new InvalidSessionException("Refresh token invalid or expired");
    }

    UUID userId = stored.getUserId();
    Optional<UserEntity> userOpt = userRepository.getUserById(userId);
    if (userOpt.isEmpty()) {
      refreshTokenService.revoke(refreshToken);
      throw new InvalidSessionException("User not found for refresh token");
    }

    UserEntity user = userOpt.get();

    SessionDto newSession = userSessionService.createSession(user);
    RefreshTokenEntity newRefresh = refreshTokenService.create(userId);
    refreshTokenService.revoke(refreshToken);

    return newSession.token();
  }

  public void updateUserInfo(UpdateUserInfoDto updateUserInfoDto) throws UserNotFoundException {
    UUID id = userSessionService.extractUserIdFromToken(updateUserInfoDto.accessToken());
    Optional<UserEntity> userOpt = userRepository.getUserById(id);
    UserEntity entity = userOpt.orElseThrow();

    if (updateUserInfoDto.newName().isPresent()) {
      entity.setName(String.valueOf(updateUserInfoDto.newName()));
    }
    if (updateUserInfoDto.newPassword().isPresent()) {
      entity.setPassword(String.valueOf(updateUserInfoDto.newPassword()));
    }
    userRepository.updateInfo(id,updateUserInfoDto.newName(), updateUserInfoDto.oldPassword(), updateUserInfoDto.newPassword());

  }
   }
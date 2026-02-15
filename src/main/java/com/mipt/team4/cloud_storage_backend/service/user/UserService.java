package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LoginRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RegisterRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.SessionDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.SimpleUserRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TokenPairDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UpdateUserInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.model.user.entity.RefreshTokenEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import com.mipt.team4.cloud_storage_backend.service.user.security.RefreshTokenService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final UserSessionService userSessionService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordHasher passwordHasher;
  private final StorageConfig storageConfig;

  public UserService(
      UserRepository userRepository,
      UserSessionService userSessionService,
      RefreshTokenService refreshTokenService,
      PasswordHasher passwordHasher,
      StorageConfig storageConfig) {
    this.passwordHasher = passwordHasher;
    this.storageConfig = storageConfig;
    this.userRepository = userRepository;
    this.userSessionService = userSessionService;
    this.refreshTokenService = refreshTokenService;
  }

  public UserDto getUserInfo(SimpleUserRequestDto getUserInfoRequest) throws UserNotFoundException {
    String token = getUserInfoRequest.token();
    UUID userId = userSessionService.extractUserIdFromToken(token);
    Optional<UserEntity> userOpt = userRepository.getUserById(userId);

    if (userOpt.isEmpty()) {
      throw new UserNotFoundException(token);
    }

    UserEntity userEntity = userOpt.get();

    return new UserDto(
        null,
        userEntity.getName(),
        userEntity.getEmail(),
        null,
        userEntity.getStorageLimit(),
        userEntity.getUsedStorage(),
        null,
        userEntity.isActive());
  }

  public TokenPairDto registerUser(RegisterRequestDto registerRequest)
      throws UserAlreadyExistsException {
    if (userRepository.getUserByEmail(registerRequest.email()).isPresent()) {
      throw new UserAlreadyExistsException(registerRequest.email());
    }

    String hash = passwordHasher.hash(registerRequest.password());
    UserEntity userEntity =
        new UserEntity(
            UUID.randomUUID(),
            registerRequest.userName(),
            registerRequest.email(),
            hash,
            storageConfig.quotas().defaultStorageLimit(),
            LocalDateTime.now());

    userRepository.addUser(userEntity);

    SessionDto session = userSessionService.createSession(userEntity);
    RefreshTokenEntity refreshToken = refreshTokenService.create(userEntity.getId());

    return new TokenPairDto(session.token(), refreshToken.token());
  }

  public TokenPairDto loginUser(LoginRequestDto loginRequest)
      throws WrongPasswordException, InvalidEmailOrPassword {
    Optional<UserEntity> userOpt = userRepository.getUserByEmail(loginRequest.email());
    if (userOpt.isEmpty()) {
      throw new InvalidEmailOrPassword();
    }

    UserEntity user = userOpt.get();
    if (!passwordHasher.verify(loginRequest.password(), user.getPasswordHash())) {
      throw new WrongPasswordException();
    }

    Optional<SessionDto> session = userSessionService.findSessionByEmail(user.getEmail());
    SessionDto usedSession;
    usedSession = session.orElseGet(() -> userSessionService.createSession(user));
    RefreshTokenEntity refreshToken = refreshTokenService.create(user.getId());

    return new TokenPairDto(usedSession.token(), refreshToken.token());
  }

  public void logoutUser(SimpleUserRequestDto logoutRequest)
      throws UserNotFoundException, InvalidSessionException {
    String token = logoutRequest.token();

    if (userSessionService.tokenExists(token)) {
      UUID userId = userSessionService.extractUserIdFromToken(token);
      refreshTokenService.revokeAllForUser(userId);

      userSessionService.blacklistToken(token);
    } else {
      throw new UserNotFoundException(token);
    }
  }

  public TokenPairDto refreshTokens(RefreshTokenDto refreshTokenRequest)
      throws InvalidSessionException {
    String refreshToken = refreshTokenRequest.refreshToken();
    RefreshTokenEntity stored = refreshTokenService.validate(refreshToken);

    if (stored == null) {
      throw new InvalidSessionException("Refresh token invalid or expired");
    }

    UUID userId = stored.userId();
    Optional<UserEntity> userOpt = userRepository.getUserById(userId);

    if (userOpt.isEmpty()) {
      refreshTokenService.revoke(refreshToken);
      throw new InvalidSessionException("User not found for refresh token");
    }

    UserEntity user = userOpt.get();

    userSessionService.revokeAllUserSessions(userId);
    SessionDto newSession = userSessionService.createSession(user);

    RefreshTokenEntity newRefreshToken = refreshTokenService.create(userId);
    refreshTokenService.revoke(refreshToken);

    return new TokenPairDto(newSession.token(), newRefreshToken.token());
  }

  public void updateUserInfo(UpdateUserInfoDto updateUserInfoDto)
      throws UserNotFoundException, WrongPasswordException {

    UUID id = userSessionService.extractUserIdFromToken(updateUserInfoDto.userToken());
    Optional<UserEntity> userOpt = userRepository.getUserById(id);
    UserEntity entity =
        userOpt.orElseThrow(() -> new UserNotFoundException(updateUserInfoDto.userToken()));

    if (updateUserInfoDto.oldPassword().isPresent()) {
      String oldPassword = updateUserInfoDto.oldPassword().get();
      String currentPasswordHash = entity.getPasswordHash();

      if (!passwordHasher.verify(oldPassword, currentPasswordHash)) {
        throw new WrongPasswordException();
      }
    }

    if (updateUserInfoDto.newPassword().isPresent()) {
      String newPasswordHash = passwordHasher.hash(updateUserInfoDto.newPassword().get());
      entity.setPasswordHash(newPasswordHash);
    }

    if (updateUserInfoDto.newName().isPresent()) {
      entity.setName(updateUserInfoDto.newName().get());
    }

    // Обновляем в репозитории
    userRepository.updateInfo(
        id,
        updateUserInfoDto.newName().orElse(entity.getName()),
        updateUserInfoDto.newPassword().map(passwordHasher::hash).orElse(entity.getPasswordHash()));
  }
}

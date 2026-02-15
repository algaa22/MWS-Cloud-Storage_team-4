package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.SessionDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TokenPairDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.LoginRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.RefreshTokenRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.RegisterRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SimpleUserRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdateUserInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import com.mipt.team4.cloud_storage_backend.service.user.security.RefreshTokenService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final UserSessionService userSessionService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordHasher passwordHasher;
  private final StorageConfig storageConfig;

  public UserDto getUserInfo(SimpleUserRequest getUserInfoRequest) throws UserNotFoundException {
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

  public TokenPairDto registerUser(RegisterRequest registerRequest)
      throws UserAlreadyExistsException {
    if (userRepository.getUserByEmail(registerRequest.email()).isPresent()) {
      throw new UserAlreadyExistsException(registerRequest.email());
    }

    String hash = passwordHasher.hash(registerRequest.password());
    UserEntity userEntity =
        UserEntity.builder()
            .id(UUID.randomUUID())
            .name(registerRequest.userName())
            .email(registerRequest.email())
            .passwordHash(hash)
            .storageLimit(storageConfig.quotas().defaultStorageLimit())
            .createdAt(LocalDateTime.now())
            .build();

    userRepository.addUser(userEntity);

    SessionDto session = userSessionService.createSession(userEntity);
    RefreshTokenDto refreshToken = refreshTokenService.create(userEntity.getId());

    return new TokenPairDto(session.token(), refreshToken.token());
  }

  public TokenPairDto loginUser(LoginRequest loginRequest)
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
    RefreshTokenDto refreshToken = refreshTokenService.create(user.getId());

    return new TokenPairDto(usedSession.token(), refreshToken.token());
  }

  public void logoutUser(SimpleUserRequest logoutRequest)
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

  public TokenPairDto refreshTokens(RefreshTokenRequest refreshTokenRequest)
      throws InvalidSessionException {
    String refreshToken = refreshTokenRequest.refreshToken();
    RefreshTokenDto stored = refreshTokenService.validate(refreshToken);

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

    RefreshTokenDto newRefreshToken = refreshTokenService.create(userId);
    refreshTokenService.revoke(refreshToken);

    return new TokenPairDto(newSession.token(), newRefreshToken.token());
  }

  public void updateUserInfo(UpdateUserInfoRequest updateUserInfoRequest)
      throws UserNotFoundException, WrongPasswordException {

    UUID id = userSessionService.extractUserIdFromToken(updateUserInfoRequest.userToken());
    Optional<UserEntity> userOpt = userRepository.getUserById(id);
    UserEntity entity =
        userOpt.orElseThrow(() -> new UserNotFoundException(updateUserInfoRequest.userToken()));

    if (updateUserInfoRequest.oldPassword().isPresent()) {
      String oldPassword = updateUserInfoRequest.oldPassword().get();
      String currentPasswordHash = entity.getPasswordHash();

      if (!passwordHasher.verify(oldPassword, currentPasswordHash)) {
        throw new WrongPasswordException();
      }
    }

    if (updateUserInfoRequest.newPassword().isPresent()) {
      String newPasswordHash = passwordHasher.hash(updateUserInfoRequest.newPassword().get());
      entity.setPasswordHash(newPasswordHash);
    }

    if (updateUserInfoRequest.newName().isPresent()) {
      entity.setName(updateUserInfoRequest.newName().get());
    }

    // Обновляем в репозитории
    userRepository.updateInfo(
        id,
        updateUserInfoRequest.newName().orElse(entity.getName()),
        updateUserInfoRequest
            .newPassword()
            .map(passwordHasher::hash)
            .orElse(entity.getPasswordHash()));
  }
}

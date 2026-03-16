package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.MissingOldPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.SessionDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.LoginRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.LogoutRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.RefreshTokenRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.RegisterRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdateUserInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UserInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.responses.TokenPairResponse;
import com.mipt.team4.cloud_storage_backend.model.user.dto.responses.UserInfoResponse;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import com.mipt.team4.cloud_storage_backend.service.user.security.RefreshTokenService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserJpaRepositoryAdapter userRepository;
  private final UserSessionService userSessionService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordHasher passwordHasher;
  private final StorageConfig storageConfig;
  private final TariffService tariffService;

  @Transactional(readOnly = true)
  public UserInfoResponse getUserInfo(UserInfoRequest request) {
    UUID userId = request.userId();
    UserEntity userEntity =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    return new UserInfoResponse(
        null,
        userEntity.getUsername(),
        userEntity.getEmail(),
        null,
        userEntity.getStorageLimit(),
        userEntity.getUsedStorage(),
        null,
        userEntity.isActive());
  }

  @Transactional
  public TokenPairResponse registerUser(RegisterRequest request) {
    if (userRepository.getUserByEmail(request.email()).isPresent()) {
      throw new UserAlreadyExistsException(request.email());
    }

    String hash = passwordHasher.hash(request.password());
    UserEntity userEntity =
        UserEntity.builder()
            .username(request.username())
            .email(request.email())
            .passwordHash(hash)
            .storageLimit(storageConfig.quotas().defaultStorageLimit())
            .createdAt(LocalDateTime.now())
            .build();

    userRepository.addUser(userEntity);

    SessionDto session = userSessionService.createSession(userEntity);
    RefreshTokenDto refreshToken = refreshTokenService.create(userEntity.getId());
    tariffService.setupTrialPeriod(userEntity.getId());

    return new TokenPairResponse(session.token(), refreshToken.token());
  }

  @Transactional
  public TokenPairResponse loginUser(LoginRequest request) {
    UserEntity userEntity =
        userRepository.getUserByEmail(request.email()).orElseThrow(InvalidEmailOrPassword::new);

    if (!passwordHasher.verify(request.password(), userEntity.getPasswordHash())) {
      throw new WrongPasswordException();
    }

    Optional<SessionDto> session = userSessionService.findSessionByEmail(userEntity.getEmail());
    SessionDto usedSession = session.orElseGet(() -> userSessionService.createSession(userEntity));
    RefreshTokenDto refreshToken = refreshTokenService.create(userEntity.getId());

    return new TokenPairResponse(usedSession.token(), refreshToken.token());
  }

  @Transactional
  public void logoutUser(LogoutRequest request) {
    refreshTokenService.revokeAllForUser(request.userId());
    userSessionService.blacklistToken(request.authToken());
  }

  @Transactional()
  public TokenPairResponse refreshTokens(RefreshTokenRequest request) {
    String refreshToken = request.refreshToken();
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

    return new TokenPairResponse(newSession.token(), newRefreshToken.token());
  }

  @Transactional
  public void updateUserInfo(UpdateUserInfoRequest request) {
    UserEntity userEntity =
        userRepository
            .getUserById(request.userId())
            .orElseThrow(() -> new UserNotFoundException(request.userId()));

    if (request.newPassword() != null) {
      if (request.oldPassword() == null) {
        throw new MissingOldPasswordException();
      } else if (!passwordHasher.verify(request.oldPassword(), userEntity.getPasswordHash())) {
        throw new WrongPasswordException();
      }

      String newPasswordHash = passwordHasher.hash(request.newPassword());
      userEntity.setPasswordHash(newPasswordHash);
    }

    if (request.newUsername() != null) {
      userEntity.setUsername(request.newUsername());
    }
  }
}

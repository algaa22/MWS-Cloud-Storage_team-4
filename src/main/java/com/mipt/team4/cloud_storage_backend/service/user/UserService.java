package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.MissingOldPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.user.auth.InvalidRefreshTokenException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TokenPairDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserSessionDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.LoginRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.LogoutRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.RefreshTokenRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.RegisterRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdateUserInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UserInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.responses.UserInfoResponse;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.model.user.enums.UserStatus;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import com.mipt.team4.cloud_storage_backend.service.user.security.RefreshTokenService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
  private final UserJpaRepositoryAdapter userRepository;
  private final UserSessionService userSessionService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordHasher passwordHasher;
  private final StorageConfig storageConfig;
  private final TariffService tariffService;
  private final StorageJpaRepository storageRepository;

  private static final long FREE_STORAGE_LIMIT = 5L * 1024 * 1024 * 1024; // 5GB

  @Transactional(readOnly = true)
  public UserInfoResponse getUserInfo(UserInfoRequest request) {
    UUID userId = request.userId();
    UserEntity userEntity =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    Long actualUsedStorage = storageRepository.sumFileSizesByUserId(userId);

    boolean hasActiveTrial =
        userEntity.getTariffPlan() == null
            && userEntity.getTrialEndDate() != null
            && userEntity.getTrialEndDate().isAfter(LocalDateTime.now());

    return new UserInfoResponse(
        userEntity.getId(),
        userEntity.getUsername(),
        userEntity.getEmail(),
        userEntity.getUserStatus(),
        userEntity.getTotalStorageLimit(),
        userEntity.getFreeStorageLimit(),
        actualUsedStorage,
        userEntity.isActive(),
        userEntity.getTariffPlan(),
        userEntity.getTariffStartDate(),
        userEntity.getTariffEndDate(),
        userEntity.isAutoRenew(),
        userEntity.getPaymentMethodId(),
        hasActiveTrial,
        userEntity.getTrialStartDate(),
        userEntity.getTrialEndDate(),
        userEntity.getScheduledDeletionDate());
  }

  @Transactional
  public TokenPairDto registerUser(RegisterRequest request) {
    if (userRepository.getUserByEmail(request.email()).isPresent()) {
      throw new UserAlreadyExistsException(request.email());
    }

    String hash = passwordHasher.hash(request.password());

    UserEntity userEntity =
        UserEntity.builder()
            .username(request.username())
            .email(request.email())
            .passwordHash(hash)
            .freeStorageLimit(FREE_STORAGE_LIMIT)
            .usedStorage(0L)
            .isActive(true)
            .userStatus(UserStatus.ACTIVE)
            .build();

    userRepository.addUser(userEntity);

    log.info("User registered successfully: {}", userEntity.getEmail());

    UserSessionDto session = userSessionService.createSession(userEntity);
    RefreshTokenDto refreshToken = refreshTokenService.create(userEntity.getId());

    return new TokenPairDto(session.token(), refreshToken.token());
  }

  @Transactional
  public TokenPairDto loginUser(LoginRequest request) {
    UserEntity userEntity =
        userRepository.getUserByEmail(request.email()).orElseThrow(InvalidEmailOrPassword::new);

    if (!passwordHasher.verify(request.password(), userEntity.getPasswordHash())) {
      throw new WrongPasswordException();
    }

    log.info("User logged in: {}", userEntity.getEmail());

    Optional<UserSessionDto> session = userSessionService.findSessionByEmail(userEntity.getEmail());
    UserSessionDto usedSession =
        session.orElseGet(() -> userSessionService.createSession(userEntity));
    RefreshTokenDto refreshToken = refreshTokenService.create(userEntity.getId());

    return new TokenPairDto(usedSession.token(), refreshToken.token());
  }

  @Transactional
  public void logoutUser(LogoutRequest request) {
    refreshTokenService.revokeAllForUser(request.userId());
    userSessionService.blacklistToken(request.authToken());

    log.info("User logged out: {}", request.userId());
  }

  @Transactional()
  public TokenPairDto refreshTokens(RefreshTokenRequest request) {
    RefreshTokenDto refreshToken = refreshTokenService.validate(request.refreshToken());

    if (refreshToken == null) {
      throw new InvalidRefreshTokenException();
    }

    UUID userId = refreshToken.userId();
    UserEntity userEntity =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    userSessionService.revokeAllUserSessions(userId);
    UserSessionDto newSession = userSessionService.createSession(userEntity);

    RefreshTokenDto newRefreshToken = refreshTokenService.create(userId);
    refreshTokenService.revoke(refreshToken.token());

    log.info("Tokens refreshed for user: {}", userId);

    return new TokenPairDto(newSession.token(), newRefreshToken.token());
  }

  @Transactional
  public void updateUserInfo(UpdateUserInfoRequest request) {
    UserEntity userEntity =
        userRepository
            .getUserById(request.userId())
            .orElseThrow(() -> new UserNotFoundException(request.userId()));

    // Проверяем, не находится ли пользователь в ограниченном режиме
    if (userEntity.getUserStatus() != UserStatus.ACTIVE) {
      throw new IllegalStateException("Cannot update user info while account is restricted");
    }

    boolean updated = false;

    if (request.newPassword() != null) {
      if (request.oldPassword() == null) {
        throw new MissingOldPasswordException();
      } else if (!passwordHasher.verify(request.oldPassword(), userEntity.getPasswordHash())) {
        throw new WrongPasswordException();
      }

      String newPasswordHash = passwordHasher.hash(request.newPassword());
      userEntity.setPasswordHash(newPasswordHash);
      updated = true;
      log.info("Password updated for user: {}", request.userId());
    }

    if (request.newName() != null) {
      userEntity.setUsername(request.newName());
      updated = true;
      log.info("Username updated for user: {} -> {}", request.userId(), request.newName());
    }

    if (!updated) {
      log.debug("No updates provided for user: {}", request.userId());
    }
  }

  @Transactional(readOnly = true)
  public boolean canUserUpload(UUID userId) {
    UserEntity user =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    return user.getUserStatus() == UserStatus.ACTIVE
        && user.getUsedStorage() < user.getTotalStorageLimit();
  }

  @Transactional(readOnly = true)
  public boolean canUserModifyFiles(UUID userId) {
    UserEntity user =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    return user.getUserStatus() == UserStatus.ACTIVE;
  }

  @Transactional(readOnly = true)
  public long getAvailableStorage(UUID userId) {
    UserEntity user =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    return user.getTotalStorageLimit() - user.getUsedStorage();
  }
}

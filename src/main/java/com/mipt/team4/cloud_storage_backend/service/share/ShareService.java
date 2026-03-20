package com.mipt.team4.cloud_storage_backend.service.share;

import com.mipt.team4.cloud_storage_backend.exception.share.*;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.share.dto.*;
import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.model.share.entity.ShareRecipient;
import com.mipt.team4.cloud_storage_backend.model.share.entity.ShareRecipientId;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.share.FileShareRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.share.ShareRecipientRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService {

  private final FileShareRepositoryAdapter shareRepository;
  private final ShareRecipientRepositoryAdapter recipientRepository;
  private final StorageRepository storageRepository;
  private final UserJpaRepositoryAdapter userRepository;
  private final PasswordHasher passwordHasher;

  @Value("${app.base-url:https://localhost:8443}")
  private String baseUrl;

  private static final int TOKEN_BYTES = 24;
  private static final int DEFAULT_EXPIRY_DAYS = 7;

  @Transactional
  public ShareCreatedResponse createShare(UUID userId, CreateShareRequest request) {
    log.info("Creating share for file: {} by user: {}", request.fileId(), userId);

    StorageEntity file =
        storageRepository
            .get(userId, request.fileId())
            .orElseThrow(() -> new StorageFileNotFoundException(request.fileId()));

    UserEntity user =
        userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    String token = generateUniqueToken();

    FileShare share =
        FileShare.builder()
            .shareToken(token)
            .file(file)
            .createdBy(user)
            .shareType(
                request.shareType() != null ? request.shareType() : FileShare.ShareType.PUBLIC)
            .maxDownloads(request.maxDownloads())
            .downloadCount(0)
            .isActive(true)
            .build();

    if (request.expiresAt() != null && !request.expiresAt().isEmpty()) {
      try {
        share.setExpiresAt(LocalDateTime.parse(request.expiresAt()));
      } catch (DateTimeParseException e) {
        share.setExpiresAt(LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS));
      }
    } else {
      share.setExpiresAt(LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS));
    }

    if (request.password() != null && !request.password().isEmpty()) {
      share.setPasswordHash(passwordHasher.hash(request.password()));
      share.setShareType(FileShare.ShareType.PROTECTED);
    }

    FileShare savedShare = shareRepository.save(share);

    if (request.shareType() == FileShare.ShareType.PRIVATE && request.recipientUserIds() != null) {
      saveRecipients(savedShare, request.recipientUserIds(), request.permission());
    }

    return ShareCreatedResponse.fromShare(savedShare, baseUrl);
  }

  @Transactional(readOnly = true)
  public ShareInfoResponse getShareInfo(String token) {
    FileShare share = findActiveShare(token);
    return ShareInfoResponse.fromShare(share, baseUrl);
  }

  @Transactional
  public ShareDownloadInfo prepareDownload(String token, String password) {
    FileShare share = findActiveShare(token);

    if (share.getPasswordHash() != null) {
      if (password == null) throw new SharePasswordRequiredException(token);
      if (!passwordHasher.verify(password, share.getPasswordHash())) {
        throw new InvalidSharePasswordException();
      }
    }

    share.incrementDownloadCount();
    shareRepository.save(share);

    StorageEntity file = share.getFile();
    return ShareDownloadInfo.builder()
        .fileName(file.getName())
        .mimeType(file.getMimeType())
        .fileSize(file.getSize())
        .data(downloadFileToBytes(file))
        .requiresPassword(false)
        .shareToken(token)
        .build();
  }

  @Transactional(readOnly = true)
  public ShareDownloadInfo validatePassword(ValidatePasswordRequest request) {
    FileShare share = findActiveShare(request.shareToken());

    if (share.getPasswordHash() != null) {
      if (request.password() == null
          || !passwordHasher.verify(request.password(), share.getPasswordHash())) {
        throw new InvalidSharePasswordException();
      }
    }

    return ShareDownloadInfo.builder()
        .fileName(share.getFile().getName())
        .mimeType(share.getFile().getMimeType())
        .fileSize(share.getFile().getSize())
        .requiresPassword(false)
        .shareToken(share.getShareToken())
        .build();
  }

  @Transactional
  public void deactivateShare(UUID shareId, UUID userId) {
    FileShare share =
        shareRepository.findById(shareId).orElseThrow(() -> new ShareNotFoundException(shareId));

    if (!share.getCreatedBy().getId().equals(userId)) {
      throw new SecurityException("Only owner can deactivate share");
    }

    share.setIsActive(false);
    shareRepository.save(share);
  }

  @Transactional(readOnly = true)
  public List<ShareInfoResponse> getUserSharesInfo(UUID userId) {
    return shareRepository.findByCreatedById(userId).stream()
        .map(share -> ShareInfoResponse.fromShare(share, baseUrl))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ShareInfoResponse> getFileSharesInfo(UUID fileId, UUID userId) {
    storageRepository
        .get(userId, fileId)
        .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    return shareRepository.findByFileId(fileId).stream()
        .map(share -> ShareInfoResponse.fromShare(share, baseUrl))
        .toList();
  }

  @Scheduled(cron = "0 0 * * * *")
  @Transactional
  public void cleanupExpiredShares() {
    List<FileShare> expired = shareRepository.findExpiredShares(LocalDateTime.now());
    expired.forEach(
        share -> {
          share.setIsActive(false);
          shareRepository.save(share);
        });
    log.info("Cleaned up {} expired shares", expired.size());
  }

  private FileShare findActiveShare(String token) {
    FileShare share =
        shareRepository
            .findByShareToken(token)
            .orElseThrow(() -> new ShareNotFoundException(token));

    if (!share.getIsActive()) {
      throw new ShareExpiredException(token);
    }

    if (share.isExpired()) {
      share.setIsActive(false);
      shareRepository.save(share);
      throw new ShareExpiredException(token);
    }

    if (share.getMaxDownloads() != null && share.getDownloadCount() >= share.getMaxDownloads()) {
      throw new ShareLimitExceededException(token);
    }

    return share;
  }

  private void saveRecipients(FileShare share, List<UUID> recipientIds, String permission) {
    for (UUID recipientId : recipientIds) {
      UserEntity recipientUser =
          userRepository
              .getUserById(recipientId)
              .orElseThrow(() -> new UserNotFoundException(recipientId));

      ShareRecipient recipient =
          ShareRecipient.builder()
              .id(new ShareRecipientId(share.getId(), recipientId))
              .share(share)
              .user(recipientUser)
              .permission(permission != null ? permission : "READ")
              .build();

      recipientRepository.save(recipient);
    }
  }

  private String generateUniqueToken() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[TOKEN_BYTES];
    String token;
    do {
      random.nextBytes(bytes);
      token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    } while (shareRepository.existsByShareToken(token));
    return token;
  }

  private byte[] downloadFileToBytes(StorageEntity file) {
    try (InputStream inputStream = storageRepository.download(file)) {
      return inputStream.readAllBytes();
    } catch (IOException e) {
      log.error("Failed to read file data for: {}", file.getName(), e);
      throw new RuntimeException("Failed to read file data", e);
    }
  }
}

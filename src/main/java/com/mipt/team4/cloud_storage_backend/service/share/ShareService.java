package com.mipt.team4.cloud_storage_backend.service.share;

import com.mipt.team4.cloud_storage_backend.exception.share.*;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.share.dto.*;
import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.share.FileShareRepositoryAdapter;
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
import java.util.Optional;
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

  private static final int TOKEN_BYTES = 24;
  private static final int DEFAULT_EXPIRY_DAYS = 7;
  private final FileShareRepositoryAdapter shareRepository;
  private final StorageRepository storageRepository;
  private final UserJpaRepositoryAdapter userRepository;
  private final PasswordHasher passwordHasher;

  @Value("${app.base-url:https://localhost:8443}")
  private String baseUrl;

  @Transactional
  public ShareCreatedResponse createShare(UUID userId, CreateShareRequest request) {

    FileShare.ShareType shareType =
        request.shareType() != null ? request.shareType() : FileShare.ShareType.PUBLIC;

    boolean hasPassword = request.password() != null && !request.password().isEmpty();
    if (hasPassword) {
      shareType = FileShare.ShareType.PROTECTED;
    }

    Optional<FileShare> existingShare =
        shareRepository.findExistingActiveShareByType(request.fileId(), userId, shareType);

    if (existingShare.isPresent()) {
      FileShare activeShare = existingShare.get();

      if (!activeShare.isExpired()
          && (activeShare.getMaxDownloads() == null
              || activeShare.getDownloadCount() < activeShare.getMaxDownloads())) {

        log.info("Returning existing active {} share for file: {}", shareType, request.fileId());
        return ShareCreatedResponse.fromShare(activeShare, baseUrl);
      } else {
        activeShare.setIsActive(false);
        shareRepository.save(activeShare);
        log.info("Deactivated expired/used {} share for file: {}", shareType, request.fileId());
      }
    }

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
            .shareType(shareType)
            .maxDownloads(request.maxDownloads())
            .downloadCount(0)
            .isActive(true)
            .build();

    setExpirationDate(share, request.expiresAt());

    if (hasPassword) {
      share.setPasswordHash(passwordHasher.hash(request.password()));
    }

    FileShare savedShare = shareRepository.save(share);
    log.info("New {} share created successfully with token: {}", shareType, token);

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

    shareRepository.deactivateShare(shareId);
  }

  @Transactional(readOnly = true)
  public List<ShareInfoResponse> getUserSharesInfo(UUID userId) {
    return shareRepository.findByCreatedById(userId).stream()
        .filter(
            share -> {
              try {
                StorageEntity file = share.getFile();
                return file != null
                    && !file.isDeleted()
                    && storageRepository.get(file.getUserId(), file.getId()).isPresent();
              } catch (Exception e) {
                log.warn("Skipping share {} because file no longer exists", share.getId());
                return false;
              }
            })
        .map(share -> ShareInfoResponse.fromShare(share, baseUrl))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ShareInfoResponse> getFileSharesInfo(UUID fileId, UUID userId) {
    storageRepository
        .get(userId, fileId)
        .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    return shareRepository.findByFileId(fileId).stream()
        .filter(
            share -> {
              try {
                StorageEntity file = share.getFile();
                return file != null && !file.isDeleted();
              } catch (Exception e) {
                log.warn("Skipping share {} because file no longer exists", share.getId());
                return false;
              }
            })
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

    StorageEntity file = share.getFile();

    if (storageRepository.getIncludeDeleted(file.getUserId(), file.getId()).isEmpty()) {
      log.warn("Share {} points to deleted/non-existent file {}", token, file.getId());
      share.setIsActive(false);
      shareRepository.save(share);
      throw new ShareFileDeletedException("The file has been deleted");
    }

    if (file.isDeleted()) {
      log.warn("Share {} points to soft-deleted file {}, deactivating", token, file.getId());
      share.setIsActive(false);
      shareRepository.save(share);
      throw new ShareFileDeletedException("The file has been deleted");
    }

    return share;
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

  private void setExpirationDate(FileShare share, String expiresAt) {
    if (expiresAt != null && !expiresAt.isEmpty()) {
      try {
        share.setExpiresAt(LocalDateTime.parse(expiresAt));
        log.info("Share expiration set to: {}", expiresAt);
      } catch (DateTimeParseException e) {
        log.warn("Invalid expiration date format: {}, using default", expiresAt);
        share.setExpiresAt(LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS));
      }
    } else {
      share.setExpiresAt(LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS));
      log.info("Share expiration set to default: {} days", DEFAULT_EXPIRY_DAYS);
    }
  }

  @Transactional
  public void deleteSharePermanently(UUID shareId, UUID userId) {
    FileShare share =
        shareRepository.findById(shareId).orElseThrow(() -> new ShareNotFoundException(shareId));

    if (!share.getCreatedBy().getId().equals(userId)) {
      throw new SecurityException("Only owner can delete share");
    }

    shareRepository.deleteById(shareId);
    log.info("Share {} permanently deleted by user {}", shareId, userId);
  }
}

package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.antivirus.config.props.AntivirusProps;
import com.mipt.team4.cloud_storage_backend.antivirus.model.enums.ScanVerdict;
import com.mipt.team4.cloud_storage_backend.antivirus.service.AntivirusService;
import com.mipt.team4.cloud_storage_backend.config.props.NotificationConfig;
import com.mipt.team4.cloud_storage_backend.exception.storage.*;
import com.mipt.team4.cloud_storage_backend.exception.upload.MissingChecksumException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.tariff.TariffAccessDeniedException;
import com.mipt.team4.cloud_storage_backend.model.common.mappers.PaginationMapper;
import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ContentRangeDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfoDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedDownloadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RestoreFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.TrashFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FilePreviewResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.share.FileShareRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepositoryWrapper;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.NotificationService;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.utils.converter.ContentRangeConverter;
import com.mipt.team4.cloud_storage_backend.utils.file.HashUtils;
import com.mipt.team4.cloud_storage_backend.utils.string.MimeTypeDetector;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

  private static final int PREVIEW_EXPIRY_SECONDS = 3600;

  private final FileErasureService erasureService;
  private final AntivirusService antivirusService;
  private final TariffService tariffService;
  private final AntivirusProps antivirusProps;

  private final StorageRepository storageRepository;
  private final StorageJpaRepositoryAdapter storageJpaRepositoryAdapter;
  private final StorageRepositoryWrapper storageRepositoryWrapper;
  private final UserJpaRepositoryAdapter userRepository;
  private final NotificationService notificationService;
  private final FileShareRepositoryAdapter shareRepository;
  private final NotificationConfig notificationConfig;
  private final NotificationClient notificationClient;

  @Transactional
  public UUID simpleUpload(SimpleUploadRequest request) {
    UUID userId = request.userId();
    UUID parentId = request.parentId();
    String name = request.name();

    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException();
    }

    if (antivirusProps.enabled() && request.checksum() == null) {
      throw new MissingChecksumException();
    }

    storageRepository
        .getIncludeDeleted(userId, parentId, name)
        .ifPresent(
            entity -> {
              throw new FileAlreadyExistsException(parentId, name);
            });

    UUID fileId = UUID.randomUUID();
    String mimeType = MimeTypeDetector.detect(name);
    byte[] data = request.data();
    String serverChecksum = null;

    if (request.checksum() != null) {
      MessageDigest messageDigest = HashUtils.createSha256();
      serverChecksum = HashUtils.encodeDigest(messageDigest.digest(data));
      HashUtils.compareChecksums(request.checksum(), serverChecksum);
    }

    StorageEntity fileEntity =
        StorageEntity.builder()
            .id(fileId)
            .userId(userId)
            .mimeType(mimeType)
            .size(data.length)
            .parentId(parentId)
            .name(name)
            .isDirectory(false)
            .tags(request.tags())
            .updatedAt(LocalDateTime.now())
            .hash(serverChecksum)
            .build();

    userRepository.increaseUsedStorage(userId, data.length);
    storageRepository.add(fileEntity, data);
    antivirusService.sendToScan(fileEntity);
    notificationService.checkStorageUsageAndNotify(userId);

    return fileId;
  }

  @Transactional(readOnly = true)
  public FileDownloadInfoDto download(ChunkedDownloadRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException();
    }

    StorageEntity fileEntity =
        storageRepository.get(userId, fileId).orElseThrow(() -> new FileNotFoundException(fileId));

    if (fileEntity.isDirectory()) {
      throw new CannotDownloadDirectoryException(fileEntity.getId());
    }

    if (fileEntity.getStatus() == FileStatus.DANGEROUS) {
      throw new FileIsDangerousException(fileEntity.getId());
    }

    validateEntityNotLocked(fileEntity);

    String rangeStr = request.range();
    ContentRangeDto rangeDto =
        rangeStr != null
            ? ContentRangeConverter.fromClientRange(rangeStr, fileEntity.getSize())
            : null;

    InputStream inputStream = storageRepository.download(fileEntity, rangeStr);

    return new FileDownloadInfoDto(
        fileEntity.getMimeType(), inputStream, rangeDto, fileEntity.getSize());
  }

  @Transactional
  public void hardDelete(DeleteFileRequest request) {
    UUID fileId = request.id();
    UUID userId = request.userId();

    StorageEntity fileEntity =
        storageRepository
            .getIncludeDeleted(userId, fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));

    validateEntityNotLocked(fileEntity);

    UserEntity userEntity =
        userRepository
            .getUserById(userId)
            .orElseThrow(() -> new UserNotFoundException(request.userId()));

    erasureService.hardDelete(fileEntity);
    notificationService.notifyFileDeleted(fileId, userEntity);
  }

  @Transactional
  public void softDelete(DeleteFileRequest request) {
    UUID fileId = request.id();
    UUID userId = request.userId();

    shareRepository.deactivateAllByFileId(fileId);

    StorageEntity fileEntity =
        storageRepository
            .getIncludeDeleted(userId, fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
    userRepository.decreaseUsedStorage(userId, fileEntity.getSize());

    fileEntity.setDeleted(true);
    fileEntity.setDeletedAt(LocalDateTime.now());
    storageRepository.softDeleteEntity(fileEntity);
  }

  @Transactional
  public void restore(RestoreFileRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    StorageEntity fileEntity =
        storageRepository
            .getDeleted(userId, fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));

    if (storageRepository.exists(userId, fileEntity.getParentId(), fileEntity.getName())) {
      throw new FileAlreadyExistsException(fileEntity.getParentId(), fileEntity.getName());
    }

    storageRepository.restore(fileEntity);
    userRepository.increaseUsedStorage(userId, fileEntity.getSize());

    List<FileShare> shares = shareRepository.findByFileId(fileId);
    for (FileShare share : shares) {
      if (share.getIsActive() == false
          && share.getExpiresAt() != null
          && share.getExpiresAt().isAfter(LocalDateTime.now())) {
        share.setIsActive(true);
        shareRepository.save(share);
      }
    }
    log.info("Restored {} shares for file {}", shares.size(), fileId);
  }

  @Transactional(readOnly = true)
  public Page<StorageEntity> getTrashFileList(TrashFileListRequest request) {
    return storageRepository.getTrashFileList(
        request.userId(), request.parentId(), PaginationMapper.toPageQuery(request.pagination()));
  }

  @Transactional(readOnly = true)
  public Page<StorageEntity> getFileList(GetFileListRequest request) {
    UUID parentId = request.parentId();
    UUID userId = request.userId();

    return storageRepository.getFileList(
        new FileListFilter(
            userId, parentId, request.includeDirectories(), request.recursive(), request.tags()),
        PaginationMapper.toPageQuery(request.pagination()));
  }

  @Transactional(readOnly = true)
  public StorageEntity getInfo(GetFileInfoRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    return storageRepository
        .get(userId, fileId)
        .orElseThrow(() -> new FileNotFoundException(fileId));
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> getAllTrashFiles(UUID userId) {
    return storageRepository.getAllTrashFiles(userId);
  }

  @Transactional
  public void changeMetadata(ChangeFileMetadataRequest request) {
    UUID fileId = request.id();
    UUID userId = request.userId();

    if (!tariffService.canModifyFiles(userId)) {
      throw new TariffAccessDeniedException();
    }

    StorageEntity fileEntity =
        storageRepository.get(userId, fileId).orElseThrow(() -> new FileNotFoundException(fileId));

    if (storageRepository.exists(userId, request.newParentId(), request.newName())) {
      throw new FileAlreadyExistsException(request.newParentId(), request.newName());
    }

    if (request.newName() != null) fileEntity.setName(request.newName());
    if (request.newParentId() != null) fileEntity.setParentId(request.newParentId());
    if (request.newTags() != null) fileEntity.setTags(request.newTags());
    if (request.newVisibility() != null) fileEntity.setVisibility(request.newVisibility().name());
  }

  private void validateEntityNotLocked(StorageEntity fileEntity) {
    if (fileEntity.getScanVerdict() == ScanVerdict.SCANNING) {
      throw new FileUnderScanException(fileEntity.getId());
    }

    if (fileEntity.isDirectory()) {
      boolean hasLocked =
          storageRepository.hasLockedDescendants(fileEntity.getUserId(), fileEntity.getId());

      if (hasLocked) {
        throw new DirectoryContainsLockedFilesException(fileEntity.getId());
      }
    }
  }

  @Transactional(readOnly = true)
  public FilePreviewResponse generatePreviewUrl(UUID fileId, UUID userId) {
    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException();
    }

    StorageEntity fileEntity =
        storageRepository
            .get(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    boolean isPreviewable = isPreviewableMimeType(fileEntity.getMimeType());

    String previewUrl = null;
    if (isPreviewable) {
      previewUrl = storageRepository.generatePresignedUrl(fileEntity, PREVIEW_EXPIRY_SECONDS);
    }

    return FilePreviewResponse.from(fileEntity, previewUrl);
  }

  private boolean isPreviewableMimeType(String mimeType) {
    if (mimeType == null) return false;
    return mimeType.startsWith("image/")
        || mimeType.equals("application/pdf")
        || mimeType.startsWith("video/")
        || mimeType.equals("text/plain")
        || mimeType.startsWith("text/");
  }

  private void checkStorageAndNotify(UUID userId) {
    userRepository
        .getStorageUsage(userId)
        .ifPresent(
            usage -> {
              double ratio = usage.getRatio();

              log.info(
                  "Storage check for user {}: used={}, limit={}, {}%",
                  userId, usage.used(), usage.limit(), String.format("%.2f", ratio * 100));

              if (ratio >= notificationConfig.fullThreshold()) {
                userRepository
                    .getUserById(userId)
                    .ifPresent(
                        user ->
                            notificationClient.notifyStorageFull(
                                user.getEmail(), user.getUsername(), userId));
              } else if (ratio >= notificationConfig.almostFullThreshold()) {
                userRepository
                    .getUserById(userId)
                    .ifPresent(
                        user ->
                            notificationClient.notifyStorageAlmostFull(
                                user.getEmail(),
                                user.getUsername(),
                                usage.used(),
                                usage.limit(),
                                userId));
              }
            });
  }

  @GetMapping("/preview/content")
  public ResponseEntity<byte[]> previewContent(
      @RequestParam UUID fileId, @RequestParam UUID userId) {
    StorageEntity fileEntity =
        storageRepository
            .get(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    InputStream inputStream = storageRepository.download(fileEntity, null);

    try {
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(fileEntity.getMimeType()))
          .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
          .body(inputStream.readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

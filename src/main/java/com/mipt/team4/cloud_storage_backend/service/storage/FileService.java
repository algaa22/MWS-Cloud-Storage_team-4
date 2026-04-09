package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.config.props.MinioConfig;
import com.mipt.team4.cloud_storage_backend.config.props.NotificationConfig;
import com.mipt.team4.cloud_storage_backend.exception.retry.CompleteUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ProcessUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.UploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadNotStoppedException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionAlreadyExists;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.tariff.TariffAccessDeniedException;
import com.mipt.team4.cloud_storage_backend.model.share.entity.FileShare;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadInfoDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfoDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.UploadChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.UploadPartDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.DeleteFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileInfoRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.RestoreFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedDownloadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.share.FileShareRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepositoryWrapper;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.utils.ChunkCombiner;
import com.mipt.team4.cloud_storage_backend.utils.MimeTypeDetector;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
  // TODO: нужен Scheduler для очистки старых сессий загрузки
  private final Map<UUID, ChunkedUploadSession> activeUploads = new ConcurrentHashMap<>();

  private final FileErasureService erasureService;
  private final TariffService tariffService;

  private final StorageRepository storageRepository;
  private final StorageJpaRepositoryAdapter
      storageJpaRepositoryAdapter;
  private final StorageRepositoryWrapper
      storageRepositoryWrapper;
  private final UserJpaRepositoryAdapter userRepository;

  private final NotificationClient notificationClient;
  private final NotificationConfig notificationConfig;
  private final MinioConfig minioConfig;
  private final FileShareRepositoryAdapter shareRepository;

  private static final long FREE_STORAGE_LIMIT = 5L * 1024 * 1024 * 1024;

  @Transactional
  public ChunkedUploadInfoDto startChunkedUpload(StartChunkedUploadRequest request) {
    UUID parentId = request.parentId();
    UUID userId = request.userId();

    if (!tariffService.hasAccess(request.userId())) {
      throw new TariffAccessDeniedException("Your tariff has expired. Please renew to continue.");
    }

    UUID uploadSessionId = UUID.randomUUID();
    String name = request.name();

    if (activeUploads.containsKey(uploadSessionId)) {
      throw new UploadSessionAlreadyExists(uploadSessionId);
    }

    storageRepository
        .getIncludeDeleted(userId, parentId)
        .ifPresent(
            entity -> {
              throw new StorageFileAlreadyExistsException(parentId, name);
            });

    StorageEntity fileEntity =
        StorageEntity.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .mimeType(MimeTypeDetector.detect(name))
            .parentId(parentId)
            .name(name)
            .isDirectory(false)
            .tags(request.fileTags())
            .status(FileStatus.READY)
            .updatedAt(LocalDateTime.now())
            .build();

    userRepository.increaseUsedStorage(userId, request.fileSize());
    String uploadId = storageRepository.startMultipartUpload(fileEntity);

    activeUploads.put(uploadSessionId, new ChunkedUploadSession(request, uploadId, fileEntity));

    return new ChunkedUploadInfoDto(uploadSessionId);
  }

  public void resumeChunkedUploadSession(ChunkedUploadInfoDto uploadInfo) {
    UUID uploadSessionId = uploadInfo.sessionId();
    ChunkedUploadSession uploadState = activeUploads.get(uploadSessionId);

    if (uploadState == null) {
      throw new UploadSessionNotFoundException();
    }

    if (!uploadState.isStopped()) {
      throw new UploadNotStoppedException();
    }

    uploadState.resume();
  }

  public void uploadChunk(UploadChunkDto request) {
    ChunkedUploadSession uploadState = activeUploads.get(request.sessionId());
    if (uploadState == null) {
      throw new UploadSessionNotFoundException();
    }

    uploadState.getChunks().add(request.chunkData());
    uploadState.addPartSize(request.chunkData().length);

    if (uploadState.getPartSize() >= minioConfig.minFilePartSize()) {
      uploadPart(uploadState);
    }
  }

  public ChunkedUploadFileResponse completeChunkedUpload(ChunkedUploadInfoDto uploadInfo) {
    ChunkedUploadSession uploadState = activeUploads.get(uploadInfo.sessionId());

    if (uploadState == null) {
      throw new UploadSessionNotFoundException();
    }

    try {
      if (uploadState.getTotalParts() == 0) {
        throw new TooSmallFilePartException(minioConfig.minFilePartSize());
      }

      if (uploadState.getPartSize() != 0) {
        uploadPart(uploadState);
      }

      for (int i = 1; i <= uploadState.getTotalParts(); i++) {
        if (!uploadState.getETags().containsKey(i)) {
          throw new MissingFilePartException(i);
        }
      }
    } catch (Exception exception) {
      if (!(exception instanceof UploadRetriableException)) {
        activeUploads.remove(uploadInfo.sessionId());
      }

      throw exception;
    }

    StorageEntity fileEntity = uploadState.getEntity();

    try {
      storageRepository.completeMultipartUpload(
          fileEntity, uploadState.getFileSize(), uploadState.getUploadId(), uploadState.getETags());
    } catch (UploadRetriableException exception) {
      uploadState.stop();
      throw new CompleteUploadRetriableException(exception.getCause());
    }

    checkStorageAndNotify(fileEntity.getUserId());
    activeUploads.remove(uploadInfo.sessionId());

    return new ChunkedUploadFileResponse(
        fileEntity.getId(), fileEntity.getSize(), uploadState.getTotalParts());
  }

  public UUID simpleUpload(FileUploadRequest request) {
    UUID fileId = UUID.randomUUID();
    UUID userId = request.userId();

    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException("Your tariff has expired. Please renew to continue.");
    }

    UUID parentId = request.parentId();
    String fileName = request.name();

    Optional<StorageEntity> fileEntity = storageRepository.getIncludeDeleted(userId, fileId);

    if (fileEntity.isPresent()) {
      throw new StorageFileAlreadyExistsException(parentId, fileName);
    }

    String mimeType = MimeTypeDetector.detect(fileName);
    byte[] data = request.data();

    StorageEntity entity =
        StorageEntity.builder()
            .id(fileId)
            .userId(userId)
            .mimeType(mimeType)
            .size(data.length)
            .parentId(parentId)
            .name(fileName)
            .isDirectory(false)
            .tags(request.tags())
            .status(FileStatus.READY)
            .updatedAt(LocalDateTime.now())
            .build();

    userRepository.increaseUsedStorage(userId, data.length);
    storageRepository.add(entity, data);

    checkStorageAndNotify(userId);
    return fileId;
  }

  public FileDownloadInfoDto download(StartChunkedDownloadRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException("Your tariff has expired. Please renew to continue.");
    }

    Optional<StorageEntity> fileEntity = storageRepository.get(userId, fileId);
    StorageEntity entity = fileEntity.orElseThrow(() -> new StorageFileNotFoundException(fileId));

    return new FileDownloadInfoDto(
        fileEntity.get().getMimeType(), storageRepository.download(entity), entity.getSize());
  }

  public void hardDelete(DeleteFileRequest request) {
    System.out.println("=== HARD DELETE SERVICE ===");
    UUID fileId = request.id();
    UUID userId = request.userId();

    System.out.println("File ID: " + fileId);
    System.out.println("User ID: " + userId);

    int deactivatedShares = shareRepository.deactivateAllByFileId(fileId);
    System.out.println("Deactivated " + deactivatedShares + " shares for file: " + fileId);

    Optional<StorageEntity> fileEntity = storageRepository.getIncludeDeleted(userId, fileId);
    if (fileEntity.isEmpty()) {
      System.out.println("File not found in DB at all!");
      return;
    }

    StorageEntity entity = fileEntity.get();
    System.out.println("Found file: " + entity.getName());
    System.out.println("Is deleted: " + entity.isDeleted());

    UserEntity user = userRepository
        .getUserById(userId)
        .orElseThrow(() -> new UserNotFoundException(request.userId()));

    erasureService.hardDelete(entity);

    String fullPath = storageRepositoryWrapper.getFullFilePath(fileId);
    notificationClient.notifyFileDeleted(user.getEmail(), user.getUsername(), fullPath, userId);

    System.out.println("Hard delete completed");
  }

  @Transactional
  public void softDelete(DeleteFileRequest request) {
    UUID fileId = request.id();
    UUID userId = request.userId();

    shareRepository.deactivateAllByFileId(fileId);

    StorageEntity fileEntity =
        storageRepository
            .getIncludeDeleted(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));
    userRepository.decreaseUsedStorage(userId, fileEntity.getSize());

    fileEntity.setDeleted(true);
    fileEntity.setDeletedAt(LocalDateTime.now());
    storageRepository.softDeleteEntity(fileEntity);
  }

  @Transactional
  public void restore(RestoreFileRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    System.out.println("=== FILE SERVICE RESTORE ===");
    System.out.println("Looking for file: " + fileId);

    StorageEntity fileEntity =
        storageRepository
            .getDeletedById(userId, fileId)
            .orElseThrow(
                () -> {
                  System.out.println("File not found in trash!");
                  return new StorageFileNotFoundException(fileId);
                });

    System.out.println("Found file: " + fileEntity.getName());
    System.out.println("Current deleted status: " + fileEntity.isDeleted());
    System.out.println("Parent ID: " + fileEntity.getParentId());

    if (storageRepository.exists(userId, fileEntity.getParentId(), fileEntity.getName())) {
      System.out.println("Conflict! File already exists at destination");
      throw new StorageFileAlreadyExistsException(fileEntity.getParentId(), fileEntity.getName());
    }

    System.out.println("Calling storageRepository.restore...");
    storageRepository.restore(fileEntity);
    userRepository.increaseUsedStorage(userId, fileEntity.getSize());

    List<FileShare> shares = shareRepository.findByFileId(fileId);
    for (FileShare share : shares) {
      if (share.getIsActive() == false && share.getExpiresAt() != null
          && share.getExpiresAt().isAfter(LocalDateTime.now())) {
        share.setIsActive(true);
        shareRepository.save(share);
      }
    }
    log.info("Restored {} shares for file {}", shares.size(), fileId);

    System.out.println("✅ Increased usedStorage by: " + fileEntity.getSize());
    System.out.println("Restore completed");
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> getTrashFileList(GetFileListRequest request) {
    return storageRepository.getTrashFileList(request.userId(), request.parentId());
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> getFileList(GetFileListRequest request) {
    UUID parentId = request.parentId();
    UUID userId = request.userId();

    return storageRepository.getFileList(
        new FileListFilter(
            userId, parentId, request.includeDirectories(), request.recursive(), request.tags()));
  }

  @Transactional(readOnly = true)
  public StorageEntity getInfo(GetFileInfoRequest request) {
    UUID fileId = request.fileId();
    UUID userId = request.userId();

    Optional<StorageEntity> fileEntity = storageRepository.get(userId, fileId);
    if (fileEntity.isEmpty()) {
      throw new StorageFileNotFoundException(fileId);
    }

    return fileEntity.get();
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
      throw new TariffAccessDeniedException("Cannot modify files: subscription expired");
    }

    StorageEntity fileEntity =
        storageRepository
            .get(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    if (storageRepository.exists(userId, request.newParentId(), request.newName())) {
      throw new StorageFileAlreadyExistsException(request.newParentId(), request.newName());
    }

    if (request.newName() != null) fileEntity.setName(request.newName());
    if (request.newParentId() != null) fileEntity.setParentId(request.newParentId());
    if (request.newTags() != null) fileEntity.setTags(request.newTags());
    if (request.newVisibility() != null) fileEntity.setVisibility(request.newVisibility().name());
  }

  /**
   * Удаляет самые старые файлы пользователя до достижения размера sizeToDelete Используется при
   * просрочке подписки
   */
  private void uploadPart(ChunkedUploadSession uploadState) {
    String uploadId = uploadState.getUploadId();
    byte[] part = ChunkCombiner.combineChunksToPart(uploadState);
    StorageEntity fileEntity = uploadState.getEntity();

    log.info(
        "[CHUNK] Uploading part #{} for session {}. Size: {} bytes",
        uploadState.getPartNum(),
        uploadState.getEntity().getId(),
        part.length);

    String eTag;

    try {
      eTag =
          storageRepository.uploadPart(
              fileEntity,
              new UploadPartDto(
                  uploadId,
                  fileEntity.getUserId(),
                  fileEntity.getId(),
                  uploadState.getPartNum(),
                  part));
      log.info("[CHUNK] Part #{} uploaded successfully. ETag: {}", uploadState.getPartNum(), eTag);
    } catch (UploadRetriableException exception) {
      log.warn(
          "[RETRY] Failed to upload part #{}. Session stopped for retry.",
          uploadState.getPartNum());
      uploadState.stop();
      throw new ProcessUploadRetriableException(
          uploadState.getFileSize(), uploadState.getPartNum(), exception.getCause());
    } finally {
      uploadState.getChunks().clear();
      uploadState.resetPartSize();
    }

    uploadState.getETags().put(uploadState.getPartNum(), eTag);
    uploadState.addFileSize(part.length);
    uploadState.increaseTotalParts();
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
}

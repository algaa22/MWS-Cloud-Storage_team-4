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
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.tariff.TariffAccessDeniedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResult;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ResumeChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleFileOperationRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SoftDeleteFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadChunkRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FileDownloadResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
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
  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();

  private final UserSessionService userSessionService;
  private final FileErasureService erasureService;
  private final TariffService tariffService;

  private final StorageRepository storageRepository;
  private final UserJpaRepositoryAdapter userRepository;

  private final NotificationClient notificationClient;
  private final NotificationConfig notificationConfig;
  private final MinioConfig minioConfig;

  public void startChunkedUpload(StartChunkedUploadRequest request) {
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
    UUID parentId = request.parentId().map(UUID::fromString).orElse(null);

    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException("Your tariff has expired. Please renew to continue.");
    }

    String uploadSessionId = request.sessionId();
    String name = request.name();

    if (activeUploads.containsKey(uploadSessionId)) {
      throw new StorageFileAlreadyExistsException(parentId, name); // TODO: не тот exception
    }

    Optional<StorageEntity> fileEntity = storageRepository.getIncludeDeleted(userId, parentId);
    if (fileEntity.isPresent()) {
      throw new StorageFileAlreadyExistsException(parentId, name);
    }

    userRepository.increaseUsedStorage(userId, request.size());

    activeUploads.put(
        request.sessionId(),
        new ChunkedUploadState(
            request,
            StorageEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .mimeType(MimeTypeDetector.detect(name))
                .parentId(parentId)
                .name(name)
                .isDirectory(false)
                .tags(request.tags())
                .status(FileStatus.READY)
                .updatedAt(LocalDateTime.now())
                .build()));
  }

  public void resumeChunkedUploadSession(ResumeChunkedUploadRequest request) {
    String uploadSessionId = request.sessionId();
    ChunkedUploadState uploadState = activeUploads.get(uploadSessionId);

    if (uploadState == null) {
      throw new UploadSessionNotFoundException();
    }

    if (!uploadState.isStopped()) {
      throw new UploadNotStoppedException();
    }

    uploadState.resume();
  }

  public void uploadChunk(UploadChunkRequest request) {
    ChunkedUploadState uploadState = activeUploads.get(request.sessionId());
    if (uploadState == null) {
      throw new UploadSessionNotFoundException();
    }

    uploadState.getChunks().add(request.chunkData());
    uploadState.addPartSize(request.chunkData().length);

    if (uploadState.getPartSize() >= minioConfig.minFilePartSize()) {
      uploadPart(uploadState);
    }
  }

  public ChunkedUploadFileResult completeChunkedUpload(String sessionId) {
    ChunkedUploadState uploadState = activeUploads.get(sessionId);
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
        activeUploads.remove(sessionId);
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

    activeUploads.remove(sessionId);

    return new ChunkedUploadFileResult(
        fileEntity.getId(), fileEntity.getSize(), uploadState.getTotalParts());
  }

  public UUID uploadFile(FileUploadRequest request) {
    UUID fileId = UUID.randomUUID();
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException("Your tariff has expired. Please renew to continue.");
    }

    UUID parentId = request.parentId().map(UUID::fromString).orElse(null);
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

  public FileDownloadResponse downloadFile(SimpleFileOperationRequest request) {
    UUID fileId = UUID.fromString(request.fileId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
    if (!tariffService.hasAccess(userId)) {
      throw new TariffAccessDeniedException("Your tariff has expired. Please renew to continue.");
    }
    Optional<StorageEntity> fileEntity = storageRepository.get(userId, fileId);
    StorageEntity entity = fileEntity.orElseThrow(() -> new StorageFileNotFoundException(fileId));

    return new FileDownloadResponse(
        fileEntity.get().getMimeType(), storageRepository.download(entity), entity.getSize());
  }

  public void hardDeleteFile(SimpleFileOperationRequest request) {
    UUID fileId = UUID.fromString(request.fileId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    Optional<StorageEntity> fileEntity = storageRepository.getIncludeDeleted(userId, fileId);
    StorageEntity entity = fileEntity.orElseThrow(() -> new StorageFileNotFoundException(fileId));

    UserEntity user =
        userRepository
            .getUserById(userId)
            .orElseThrow(() -> new UserNotFoundException(request.userToken()));

    erasureService.hardDelete(entity);

    String fullPath = storageRepository.getFullFilePath(fileId);
    notificationClient.notifyFileDeleted(user.getEmail(), user.getUsername(), fullPath, userId);
  }

  @Transactional
  public void softDeleteFile(SoftDeleteFileRequest request) {
    UUID fileId = UUID.fromString(request.fileId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    Optional<StorageEntity> fileEntity = storageRepository.getIncludeDeleted(userId, fileId);
    StorageEntity entity = fileEntity.orElseThrow(() -> new StorageFileNotFoundException(fileId));

    storageRepository.softDeleteEntity(entity);
  }

  @Transactional
  public void restoreFile(SimpleFileOperationRequest request) {
    UUID fileId = UUID.fromString(request.fileId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    StorageEntity fileEntity =
        storageRepository
            .getDeletedById(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    if (storageRepository.exists(userId, fileEntity.getParentId(), fileEntity.getName())) {
      throw new StorageFileAlreadyExistsException(fileEntity.getParentId(), fileEntity.getName());
    }

    storageRepository.restore(fileEntity);
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> getTrashFileList(GetFileListRequest request) {
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
    UUID parentId = request.parentId().map(UUID::fromString).orElse(null);

    return storageRepository.getTrashFileList(userId, parentId);
  }

  @Transactional(readOnly = true)
  public List<StorageEntity> getFileList(GetFileListRequest request) {
    UUID parentId = request.parentId().map(UUID::fromString).orElse(null);
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    return storageRepository.getFileList(
        new FileListFilter(userId, parentId, request.includeDirectories(), request.recursive()));
  }

  @Transactional(readOnly = true)
  public StorageDto getFileInfo(SimpleFileOperationRequest request) {
    UUID fileId = UUID.fromString(request.fileId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    Optional<StorageEntity> fileEntity = storageRepository.get(userId, fileId);
    if (fileEntity.isEmpty()) {
      throw new StorageFileNotFoundException(fileId);
    }

    return new StorageDto(fileEntity.get());
  }

  @Transactional
  public void changeFileMetadata(ChangeFileMetadataRequest request) {
    UUID fileId = UUID.fromString(request.fileId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    StorageEntity fileEntity =
        storageRepository
            .get(userId, fileId)
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));

    String targetName = request.newName().orElse(fileEntity.getName());
    UUID targetParentId =
        request.newParentId().isPresent()
            ? UUID.fromString(request.newParentId().get())
            : fileEntity.getParentId();

    if (request.newName().isPresent() || request.newParentId().isPresent()) {
      if (storageRepository.exists(userId, targetParentId, targetName)) {
        throw new StorageFileAlreadyExistsException(targetParentId, targetName);
      }

      fileEntity.setName(targetName);
      fileEntity.setParentId(targetParentId);
    }

    request.tags().ifPresent(fileEntity::setTags);
    request.visibility().ifPresent(fileEntity::setVisibility);
  }

  private void uploadPart(ChunkedUploadState uploadState) {
    String uploadId = uploadState.getOrCreateUploadId(storageRepository);
    byte[] part = ChunkCombiner.combineChunksToPart(uploadState);
    StorageEntity fileEntity = uploadState.getEntity();

    String eTag;

    try {
      eTag =
          storageRepository.uploadPart(
              fileEntity,
              new UploadPartRequest(
                  uploadId,
                  fileEntity.getUserId(),
                  fileEntity.getId(),
                  uploadState.getPartNum(),
                  part));
    } catch (UploadRetriableException exception) {
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

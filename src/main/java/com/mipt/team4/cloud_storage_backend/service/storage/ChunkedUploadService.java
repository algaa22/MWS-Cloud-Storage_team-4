package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.config.props.NotificationConfig;
import com.mipt.team4.cloud_storage_backend.exception.retry.CompleteUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ProcessUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.UploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionAlreadyExists;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.tariff.TariffAccessDeniedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadPartDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedUploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.CompleteChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.ChunkedUploadFileResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.ChunkedUploadInfoResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserJpaRepositoryAdapter;
import com.mipt.team4.cloud_storage_backend.service.user.TariffService;
import com.mipt.team4.cloud_storage_backend.utils.MimeTypeDetector;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChunkedUploadService {
  private final TariffService tariffService;

  private final StorageRepository storageRepository;
  private final UserJpaRepositoryAdapter userRepository;

  private final NotificationClient notificationClient;
  private final NotificationConfig notificationConfig;

  @Transactional
  public ChunkedUploadInfoResponse startChunkedUpload(StartChunkedUploadRequest request) {
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

    activeUploads.put(uploadSessionId, new ChunkedUploadSession(uploadId, fileEntity));

    return new ChunkedUploadInfoResponse(uploadSessionId);
  }

  public boolean isPartAlreadyUploaded(ChunkedUploadPartRequest request) {
    return storageRepository.isPartAlreadyUploaded(request.sessionId(), request.part());
  }

  public void uploadPart(ChunkedUploadPartDto part) {
    // TODO
  }

  public UUID completeChunkedUpload(CompleteChunkedUploadRequest request) {
    ChunkedUploadSession uploadState = activeUploads.get(request.sessionId());

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
        activeUploads.remove(request.sessionId());
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
    activeUploads.remove(request.sessionId());

    return new ChunkedUploadFileResponse(
        fileEntity.getId(), fileEntity.getSize(), uploadState.getTotalParts());
  }

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
      throw new ProcessUploadRetriableException(uploadState.getPartNum(), exception.getCause());
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

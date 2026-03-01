package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.config.props.MinioConfig;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleFileOperationRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadChunkRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.ChunkedUploadFileResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FileDownloadResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileService {
  // TODO: нужен Scheduler для очистки старых сессий загрузки
  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();

  private final UserSessionService userSessionService;
  private final StorageRepository storageRepository;
  private final UserRepository userRepository;
  private final MinioConfig minioConfig;

  public void startChunkedUploadSession(FileChunkedUploadRequest request) {
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
    UUID parentId = request.parentId().map(UUID::fromString).orElse(null);
    String uploadSessionId = request.sessionId();
    String name = request.name();

    if (activeUploads.containsKey(uploadSessionId)) {
      throw new StorageFileAlreadyExistsException(parentId, name);
    }

    Optional<StorageEntity> fileEntity = storageRepository.getFile(userId, parentId, name);
    if (fileEntity.isPresent()) {
      throw new StorageFileAlreadyExistsException(parentId, name);
    }

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

  public void uploadChunk(UploadChunkRequest request) {
    ChunkedUploadState uploadState = activeUploads.get(request.sessionId());
    if (uploadState == null) {
      throw new UploadSessionNotFoundException(request.sessionId());
    }

    uploadState.chunks.add(request.chunkData());
    uploadState.addPartSize(request.chunkData().length);

    if (uploadState.getPartSize() >= minioConfig.minFilePartSize()) {
      uploadPart(uploadState);
    }
  }

  public ChunkedUploadFileResponse completeChunkedUpload(String sessionId) {
    ChunkedUploadState uploadState = activeUploads.get(sessionId);
    if (uploadState == null) {
      throw new UploadSessionNotFoundException(sessionId);
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

      StorageEntity fileEntity = uploadState.getEntity();

      storageRepository.completeMultipartUpload(
          fileEntity, uploadState.getFileSize(), uploadState.getUploadId(), uploadState.getETags());
      userRepository.increaseUsedStorage(fileEntity.getUserId(), uploadState.getFileSize());

      return new ChunkedUploadFileResponse(
          fileEntity.getId(), fileEntity.getSize(), uploadState.getTotalParts());
    } finally {
      activeUploads.remove(sessionId);
    }
  }

  public UUID uploadFile(FileUploadRequest request) {
    UUID fileId = UUID.randomUUID();
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    String fileName = request.name();
    UUID parentId = request.parentId().map(UUID::fromString).orElse(null);

    if (storageRepository.fileExists(userId, parentId, fileName)) {
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

    storageRepository.addFile(entity, data);
    userRepository.increaseUsedStorage(userId, data.length);

    return fileId;
  }

  public FileDownloadResponse downloadFile(SimpleFileOperationRequest request) {
    UUID fileId = UUID.fromString(request.fileId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, fileId);
    StorageEntity entity = entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileId));

    return new FileDownloadResponse(
        entityOpt.get().getMimeType(), storageRepository.downloadFile(entity), entity.getSize());
  }

  public void deleteFile(SimpleFileOperationRequest request) {
    UUID fileId = UUID.fromString(request.fileId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, fileId);
    StorageEntity entity = entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileId));

    storageRepository.deleteFile(entity);
    userRepository.decreaseUsedStorage(userId, entity.getSize());
  }

  public List<StorageEntity> getFileList(GetFileListRequest request) {
    UUID parentId = request.parentId().map(UUID::fromString).orElse(null);
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    return storageRepository.getFileList(
        new FileListFilter(userId, parentId, request.includeDirectories(), request.recursive()));
  }

  public StorageDto getFileInfo(SimpleFileOperationRequest request) {
    UUID fileId = UUID.fromString(request.fileId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, fileId);
    if (entityOpt.isEmpty()) {
      throw new StorageFileNotFoundException(fileId);
    }

    return new StorageDto(entityOpt.get());
  }

  public void changeFileMetadata(ChangeFileMetadataRequest request) {
    UUID fileId = UUID.fromString(request.fileId());
    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    StorageEntity entity =
        storageRepository
            .getFile(userId, fileId)
            .filter(f -> f.getUserId().equals(userId))
            .orElseThrow(() -> new StorageFileNotFoundException(fileId));
    String targetName = request.newName().orElse(entity.getName());
    UUID targetParentId =
        request.newParentId().isPresent()
            ? UUID.fromString(request.newParentId().get())
            : entity.getParentId();

    if (request.newName().isPresent() || request.newParentId().isPresent()) {
      if (storageRepository.fileExists(userId, targetParentId, targetName)) {
        throw new StorageFileAlreadyExistsException(targetParentId, targetName);
      }
      entity.setName(targetName);
      entity.setParentId(targetParentId);
    }

    request.tags().ifPresent(entity::setTags);
    request.visibility().ifPresent(entity::setVisibility);

    storageRepository.updateFile(entity);
  }

  private void uploadPart(ChunkedUploadState uploadState) {
    String uploadId = uploadState.getOrCreateUploadId(storageRepository);
    byte[] part = ChunkCombiner.combineChunksToPart(uploadState);
    StorageEntity entity = uploadState.getEntity();

    String eTag =
        storageRepository.uploadPart(
            entity,
            new UploadPartRequest(
                uploadId, entity.getUserId(), entity.getId(), uploadState.getPartNum(), part));

    uploadState.addCompletedPart(uploadState.getPartNum(), eTag);
    uploadState.addFileSize(part.length);
    uploadState.increaseTotalParts();
  }
}

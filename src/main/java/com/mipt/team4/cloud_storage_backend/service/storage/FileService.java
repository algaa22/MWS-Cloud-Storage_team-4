package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.config.props.MinioConfig;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.CombineChunksToPartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResultDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleFileOperationRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadChunkRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import com.mipt.team4.cloud_storage_backend.utils.ChunkCombiner;
import com.mipt.team4.cloud_storage_backend.utils.MimeTypeDetector;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileService {
  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();

  private final UserSessionService userSessionService;
  private final StorageRepository storageRepository;
  private final UserRepository userRepository;
  private final MinioConfig minioConfig;

  public void startChunkedUploadSession(FileChunkedUploadRequest uploadSession)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    UUID userId = userSessionService.extractUserIdFromToken(uploadSession.userToken());
    String uploadSessionId = uploadSession.sessionId();
    UUID parentId = uploadSession.parentId();
    String name = uploadSession.name();

    if (activeUploads.containsKey(uploadSessionId)) {
      throw new StorageFileAlreadyExistsException(parentId, name);
    }

    Optional<StorageEntity> fileEntity = storageRepository.getFile(userId, parentId, name);
    if (fileEntity.isPresent()) {
      throw new StorageFileAlreadyExistsException(parentId, name);
    }

    activeUploads.put(
        uploadSession.sessionId(),
        new ChunkedUploadState(
            uploadSession,
            StorageEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .mimeType(MimeTypeDetector.detect(name))
                .parentId(parentId)
                .name(name)
                .isDirectory(false)
                .tags(uploadSession.tags())
                .status(FileStatus.PENDING)
                .build()));
  }

  public void uploadChunk(UploadChunkRequest uploadRequest)
      throws CombineChunksToPartException, UploadSessionNotFoundException {
    ChunkedUploadState uploadState = activeUploads.get(uploadRequest.sessionId());
    if (uploadState == null) {
      throw new UploadSessionNotFoundException(uploadRequest.sessionId());
    }

    uploadState.chunks.add(uploadRequest.chunkData());
    uploadState.addPartSize(uploadRequest.chunkData().length);

    if (uploadState.getPartSize() >= minioConfig.minFilePartSize()) {
      uploadPart(uploadState);
    }
  }

  @Transactional
  public ChunkedUploadFileResultDto completeChunkedUpload(String sessionId)
      throws TooSmallFilePartException,
          CombineChunksToPartException,
          MissingFilePartException,
          UploadSessionNotFoundException {
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

      FileChunkedUploadRequest session = uploadState.getSession();
      for (int i = 1; i <= uploadState.getTotalParts(); i++) {
        if (!uploadState.getETags().containsKey(i)) {
          throw new MissingFilePartException(i);
        }
      }

      StorageEntity fileEntity = uploadState.getEntity();

      storageRepository.completeMultipartUpload(
          fileEntity, uploadState.getFileSize(), uploadState.getUploadId(), uploadState.getETags());
      userRepository.increaseUsedStorage(fileEntity.getUserId(), uploadState.getFileSize());

      return new ChunkedUploadFileResultDto(
          session.parentId(),
          session.name(),
          uploadState.getFileSize(),
          uploadState.getTotalParts());
    } finally {
      activeUploads.remove(sessionId);
    }
  }

  public void uploadFile(FileUploadRequest fileUploadRequest)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    UUID fileId = UUID.randomUUID();
    UUID userId = userSessionService.extractUserIdFromToken(fileUploadRequest.userToken());

    if (storageRepository.fileExists(
        userId, fileUploadRequest.parentId(), fileUploadRequest.name())) {
      throw new StorageFileAlreadyExistsException(
          fileUploadRequest.parentId(), fileUploadRequest.name());
    }

    String mimeType = MimeTypeDetector.detect(fileUploadRequest.name());
    byte[] data = fileUploadRequest.data();

    StorageEntity entity =
        StorageEntity.builder()
            .id(fileId)
            .userId(userId)
            .mimeType(mimeType)
            .size(data.length)
            .parentId(fileUploadRequest.parentId())
            .name(fileUploadRequest.name())
            .isDirectory(false)
            .tags(fileUploadRequest.tags())
            .status(FileStatus.READY)
            .build();

    storageRepository.addFile(entity, data);
    userRepository.increaseUsedStorage(userId, data.length);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationRequest fileDownload)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(fileDownload.userToken());

    Optional<StorageEntity> entityOpt =
        storageRepository.getFile(userId, fileDownload.parentId(), fileDownload.name());
    StorageEntity entity =
        entityOpt.orElseThrow(
            () -> new StorageFileNotFoundException(fileDownload.parentId(), fileDownload.name()));

    return new FileDownloadDto(
        fileDownload.parentId(),
        fileDownload.name(),
        entityOpt.get().getMimeType(),
        storageRepository.downloadFile(entity),
        entity.getSize());
  }

  @Transactional
  public void deleteFile(SimpleFileOperationRequest deleteFileRequest)
      throws UserNotFoundException, StorageFileNotFoundException, FileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(deleteFileRequest.userToken());
    Optional<StorageEntity> entityOpt =
        storageRepository.getFile(userId, deleteFileRequest.parentId(), deleteFileRequest.name());
    StorageEntity entity =
        entityOpt.orElseThrow(
            () ->
                new StorageFileNotFoundException(
                    deleteFileRequest.parentId(), deleteFileRequest.name()));

    storageRepository.deleteFile(entity);
    userRepository.decreaseUsedStorage(userId, entity.getSize());
  }

  public List<StorageEntity> getFileList(GetFileListRequest request) throws UserNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(request.userToken());
    return storageRepository.getFilesList(
        new FileListFilter(
            userUuid,
            request.parentId().orElse(null),
            request.includeDirectories(),
            request.recursive()));
  }

  public StorageDto getFileInfo(SimpleFileOperationRequest fileInfoRequest)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(fileInfoRequest.userToken());

    Optional<StorageEntity> entityOpt =
        storageRepository.getFile(userUuid, fileInfoRequest.parentId(), fileInfoRequest.name());
    if (entityOpt.isEmpty()) {
      throw new StorageFileNotFoundException(fileInfoRequest.parentId(), fileInfoRequest.name());
    }

    return new StorageDto(entityOpt.get());
  }

  @Transactional
  public void changeFileMetadata(ChangeFileMetadataRequest request)
      throws UserNotFoundException,
          StorageFileNotFoundException,
          StorageFileAlreadyExistsException {

    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

    StorageEntity entity =
        storageRepository
            .getFileById(request.id())
            .filter(f -> f.getUserId().equals(userId))
            .orElseThrow(() -> new StorageFileNotFoundException(request.id()));
    String targetName = request.newName().orElse(entity.getName());
    UUID targetParentId =
        request.newParentId().isPresent() ? request.newParentId().get() : entity.getParentId();

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

  private void uploadPart(ChunkedUploadState uploadState) throws CombineChunksToPartException {
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

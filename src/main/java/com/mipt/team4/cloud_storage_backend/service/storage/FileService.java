package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.CombineChunksToPartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.FileMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChangeFileMetadataDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResultDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.GetFileListDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.SimpleFileOperationDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.UploadChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.UploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileVisibility;
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

public class FileService {

  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();
  private final UserSessionService userSessionService;
  private final StorageRepository storageRepository;
  private final UserRepository userRepository;

  public FileService(
      StorageRepository storageRepository,
      UserRepository userRepository,
      UserSessionService userSessionService) {
    this.storageRepository = storageRepository;
    this.userSessionService = userSessionService;
    this.userRepository = userRepository;
  }

  public void startChunkedUploadSession(FileChunkedUploadDto uploadSession)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    UUID userId = userSessionService.extractUserIdFromToken(uploadSession.userToken());
    String uploadSessionId = uploadSession.sessionId();
    String path = uploadSession.path();

    if (activeUploads.containsKey(uploadSessionId)) {
      throw new StorageFileAlreadyExistsException(path);
    }

    Optional<StorageEntity> fileEntity = storageRepository.getFile(userId, path);
    if (fileEntity.isPresent()) {
      throw new StorageFileAlreadyExistsException(path);
    }

    UUID newFileId = UUID.randomUUID();

    activeUploads.put(
        uploadSession.sessionId(), new ChunkedUploadState(uploadSession, userId, newFileId, path));
  }

  public void uploadChunk(UploadChunkDto uploadRequest)
      throws CombineChunksToPartException, UploadSessionNotFoundException {
    ChunkedUploadState uploadState = activeUploads.get(uploadRequest.sessionId());
    if (uploadState == null) {
      throw new UploadSessionNotFoundException(uploadRequest.sessionId());
    }

    uploadState.chunks.add(uploadRequest.chunkData());
    uploadState.addPartSize(uploadRequest.chunkData().length);

    if (uploadState.getPartSize() >= StorageConfig.INSTANCE.getMinFilePartSize()) {
      uploadPart(uploadState);
    }
  }

  public ChunkedUploadFileResultDto completeChunkedUpload(String sessionId)
      throws StorageFileAlreadyExistsException,
      UserNotFoundException,
      TooSmallFilePartException,
      CombineChunksToPartException,
      MissingFilePartException,
      UploadSessionNotFoundException {
    ChunkedUploadState uploadState = activeUploads.get(sessionId);
    if (uploadState == null) {
      throw new UploadSessionNotFoundException(sessionId);
    }

    try {
      if (uploadState.getTotalParts() == 0) {
        throw new TooSmallFilePartException();
      }

      if (uploadState.getPartSize() != 0) {
        uploadPart(uploadState);
      }

      FileChunkedUploadDto session = uploadState.getSession();
      for (int i = 1; i <= uploadState.getTotalParts(); i++) {
        if (!uploadState.getETags().containsKey(i)) {
          throw new MissingFilePartException(i);
        }
      }

      UUID userId = userSessionService.extractUserIdFromToken(session.userToken());

      StorageEntity fileEntity =
          new StorageEntity(
              uploadState.getFileId(),
              userId,
              uploadState.getPath(),
              MimeTypeDetector.detect(session.path()),
              FileVisibility.PRIVATE.toString(),
              uploadState.getFileSize(),
              false,
              session.tags(),
              false);

      storageRepository.completeMultipartUpload(
          fileEntity, uploadState.getUploadId(), uploadState.getETags());
      userRepository.increaseUsedStorage(userId, uploadState.getFileSize());

      return new ChunkedUploadFileResultDto(
          session.path(), uploadState.getFileSize(), uploadState.getTotalParts());
    } finally {
      activeUploads.remove(sessionId);
    }
  }

  public void uploadFile(FileUploadDto fileUploadRequest)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    UUID fileId = UUID.randomUUID();
    UUID userId = userSessionService.extractUserIdFromToken(fileUploadRequest.userToken());

    if (storageRepository.fileExists(userId, fileUploadRequest.path())) {
      throw new StorageFileAlreadyExistsException(fileUploadRequest.path());
    }

    String mimeType = MimeTypeDetector.detect(fileUploadRequest.path());
    byte[] data = fileUploadRequest.data();

    StorageEntity entity =
        new StorageEntity(
            fileId,
            userId,
            fileUploadRequest.path(),
            mimeType,
            FileVisibility.PRIVATE.toString(),
            data.length,
            false,
            fileUploadRequest.tags(),
            false);

    storageRepository.addFile(entity, data);
    userRepository.increaseUsedStorage(userId, data.length);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationDto fileDownload)
      throws UserNotFoundException, StorageEntityNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(fileDownload.userToken());

    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, fileDownload.path());
    StorageEntity entity =
        entityOpt.orElseThrow(() -> new StorageEntityNotFoundException(fileDownload.path()));

    return new FileDownloadDto(
        fileDownload.path(),
        entityOpt.get().getMimeType(),
        storageRepository.downloadFile(entity),
        entity.getSize());
  }

  public void deleteFile(SimpleFileOperationDto deleteFileRequest)
      throws UserNotFoundException, StorageEntityNotFoundException, FileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(deleteFileRequest.userToken());
    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, deleteFileRequest.path());
    StorageEntity entity =
        entityOpt.orElseThrow(() -> new StorageEntityNotFoundException(deleteFileRequest.path()));

    storageRepository.deleteFile(entity);
    userRepository.decreaseUsedStorage(userId, entity.getSize());
  }

  public List<StorageEntity> getFileList(GetFileListDto filePathsRequest)
      throws UserNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(filePathsRequest.userToken());
    return storageRepository.getFileList(
        new FileListFilter(userUuid, filePathsRequest.includeDirectories(),
            filePathsRequest.recursive(), filePathsRequest.searchDirectory().orElse("")));
  }

  public StorageDto getFileInfo(SimpleFileOperationDto fileInfoRequest)
      throws UserNotFoundException, StorageEntityNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(fileInfoRequest.userToken());

    Optional<StorageEntity> entityOpt = storageRepository.getFile(userUuid, fileInfoRequest.path());
    if (entityOpt.isEmpty()) {
      throw new StorageEntityNotFoundException(fileInfoRequest.path());
    }

    return FileMapper.toDto(entityOpt.get());
  }

  public void changeFileMetadata(ChangeFileMetadataDto changeFileMetadata)
      throws UserNotFoundException,
      StorageEntityNotFoundException,
      StorageFileAlreadyExistsException {

    UUID userId = userSessionService.extractUserIdFromToken(changeFileMetadata.userToken());

    Optional<StorageEntity> entityOpt =
        storageRepository.getFile(userId, changeFileMetadata.oldPath());

    StorageEntity entity =
        entityOpt.orElseThrow(
            () -> new StorageEntityNotFoundException(changeFileMetadata.oldPath()));

    if (changeFileMetadata.newPath().isPresent()) {
      Optional<StorageEntity> existingFile =
          storageRepository.getFile(userId, changeFileMetadata.newPath().get());
      if (existingFile.isPresent()) {
        throw new StorageFileAlreadyExistsException(changeFileMetadata.newPath().get());
      }

      entity.setPath(changeFileMetadata.newPath().get());
    }

    if (changeFileMetadata.tags().isPresent()) {
      entity.setTags(changeFileMetadata.tags().get());
    }

    if (changeFileMetadata.visibility().isPresent()) {
      entity.setVisibility(changeFileMetadata.visibility().get());
    }

    storageRepository.updateFile(entity);
  }

  private void uploadPart(ChunkedUploadState uploadState) throws CombineChunksToPartException {
    String uploadId = uploadState.getOrCreateUploadId(storageRepository);
    byte[] part = ChunkCombiner.combineChunksToPart(uploadState);

    String eTag =
        storageRepository.uploadPart(
            new UploadPartRequest(
                uploadId,
                uploadState.getUserId(),
                uploadState.getFileId(),
                uploadState.getPartNum(),
                part));

    uploadState.addCompletedPart(uploadState.getPartNum(), eTag);
    uploadState.addFileSize(part.length);
    uploadState.increaseTotalParts();
  }
}

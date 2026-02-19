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
    String path = uploadSession.path();

    if (activeUploads.containsKey(uploadSessionId)) {
      throw new StorageFileAlreadyExistsException(path);
    }

    Optional<StorageEntity> fileEntity = storageRepository.getFile(userId, path);
    if (fileEntity.isPresent()) {
      throw new StorageFileAlreadyExistsException(path);
    }

    activeUploads.put(
        uploadSession.sessionId(),
        new ChunkedUploadState(
            uploadSession,
            StorageEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .mimeType(MimeTypeDetector.detect(path))
                .path(path)
                .isDirectory(false)
                .tags(uploadSession.tags())
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
          fileEntity.getId(), uploadState.getFileSize(), uploadState.getUploadId(), uploadState.getETags());
      userRepository.increaseUsedStorage(fileEntity.getUserId(), uploadState.getFileSize());

      return new ChunkedUploadFileResultDto(
          session.path(), uploadState.getFileSize(), uploadState.getTotalParts());
    } finally {
      activeUploads.remove(sessionId);
    }
  }

  public void uploadFile(FileUploadRequest fileUploadRequest)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    UUID fileId = UUID.randomUUID();
    UUID userId = userSessionService.extractUserIdFromToken(fileUploadRequest.userToken());

    if (storageRepository.fileExists(userId, fileUploadRequest.path())) {
      throw new StorageFileAlreadyExistsException(fileUploadRequest.path());
    }

    String mimeType = MimeTypeDetector.detect(fileUploadRequest.path());
    byte[] data = fileUploadRequest.data();

    StorageEntity entity =
        StorageEntity.builder()
            .id(fileId)
            .userId(userId)
            .mimeType(mimeType)
            .size(data.length)
            .path(fileUploadRequest.path())
            .isDirectory(false)
            .tags(fileUploadRequest.tags())
            .build();

    storageRepository.addFile(entity, data);
    userRepository.increaseUsedStorage(userId, data.length);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationRequest fileDownload)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(fileDownload.userToken());

    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, fileDownload.path());
    StorageEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileDownload.path()));

    return new FileDownloadDto(
        fileDownload.path(),
        entityOpt.get().getMimeType(),
        storageRepository.downloadFile(entity),
        entity.getSize());
  }

  public void deleteFile(SimpleFileOperationRequest deleteFileRequest)
      throws UserNotFoundException, StorageFileNotFoundException, FileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(deleteFileRequest.userToken());
    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, deleteFileRequest.path());
    StorageEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(deleteFileRequest.path()));

    storageRepository.deleteFile(entity);
    userRepository.decreaseUsedStorage(userId, entity.getSize());
  }

  public List<StorageEntity> getFileList(GetFileListRequest filePathsRequest)
      throws UserNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(filePathsRequest.userToken());
    return storageRepository.getFileList(
        new FileListFilter(
            userUuid,
            filePathsRequest.includeDirectories(),
            filePathsRequest.recursive(),
            filePathsRequest.searchDirectory().orElse("")));
  }

  public StorageDto getFileInfo(SimpleFileOperationRequest fileInfoRequest)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(fileInfoRequest.userToken());

    Optional<StorageEntity> entityOpt = storageRepository.getFile(userUuid, fileInfoRequest.path());
    if (entityOpt.isEmpty()) {
      throw new StorageFileNotFoundException(fileInfoRequest.path());
    }

    return new StorageDto(entityOpt.get());
  }

  public void changeFileMetadata(ChangeFileMetadataRequest changeFileMetadata)
      throws UserNotFoundException,
          StorageFileNotFoundException,
          StorageFileAlreadyExistsException {

    UUID userId = userSessionService.extractUserIdFromToken(changeFileMetadata.userToken());

    Optional<StorageEntity> entityOpt =
        storageRepository.getFile(userId, changeFileMetadata.oldPath());

    StorageEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(changeFileMetadata.oldPath()));

    // TODO: Есть риск, что, пока мы обновляем entity, файл может быть удален другим потоком

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
    StorageEntity entity = uploadState.getEntity();

    String eTag =
        storageRepository.uploadPart(
            entity.getId(),
            new UploadPartRequest(
                uploadId, entity.getUserId(), entity.getId(), uploadState.getPartNum(), part));

    uploadState.addCompletedPart(uploadState.getPartNum(), eTag);
    uploadState.addFileSize(part.length);
    uploadState.increaseTotalParts();
  }
}

package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.CombineChunksToPartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.FileMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.*;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileService {
  private final StorageRepository storageRepository;
  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();
  private final UserSessionService userSessionService;

  public FileService(StorageRepository storageRepository, UserSessionService userSessionService) {
    this.storageRepository = storageRepository;
    this.userSessionService = userSessionService;
  }

  // TODO: soft delete?

  public void startChunkedUploadSession(FileChunkedUploadDto uploadSession)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    // TODO: разделить session'ы на юзеровский и файловский
    UUID userId = userSessionService.extractUserIdFromToken(uploadSession.userToken());
    String sessionId = uploadSession.sessionId();
    String path = uploadSession.path();
    if (activeUploads.containsKey(sessionId)) {
      throw new StorageFileAlreadyExistsException(path);
    }

    Optional<StorageEntity> fileEntity = storageRepository.getFile(userId, path);
    if (fileEntity.isPresent()) throw new StorageFileAlreadyExistsException(path);

    UUID newFileId = UUID.randomUUID();

    activeUploads.put(
        uploadSession.sessionId(), new ChunkedUploadState(uploadSession, userId, newFileId, path));
  }

  public void uploadChunk(UploadChunkDto uploadRequest)
      throws UserNotFoundException, CombineChunksToPartException {
    ChunkedUploadState uploadState = activeUploads.get(uploadRequest.sessionId());
    if (uploadState == null) {
      throw new RuntimeException("Upload session not found!");
    }

    uploadState.chunks.add(uploadRequest.chunkData());
    uploadState.partSize += uploadRequest.chunkData().length;

    if (uploadState.partSize >= 5 * 1024 * 1024) {
      uploadPart(uploadState);
    }
  }

  public ChunkedUploadFileResultDto completeChunkedUpload(String sessionId)
      throws StorageFileAlreadyExistsException,
          UserNotFoundException,
          TooSmallFilePartException,
          CombineChunksToPartException {
    ChunkedUploadState uploadState = activeUploads.get(sessionId);
    if (uploadState == null) throw new RuntimeException("No such upload session!");

    try {
      if (uploadState.totalParts == 0) {
        throw new TooSmallFilePartException();
      }

      if (uploadState.partSize != 0) {
        uploadPart(uploadState);
      }

      FileChunkedUploadDto session = uploadState.session;
      for (int i = 1; i <= uploadState.totalParts; i++) {
        if (!uploadState.eTags.containsKey(i)) throw new RuntimeException("Missing chunk #" + i);
      }

      UUID userId = userSessionService.extractUserIdFromToken(session.userToken());

      StorageEntity fileEntity =
          new StorageEntity(
              uploadState.fileId,
              userId, // TODO: get actualUserId
              uploadState.path,
              guessMimeType(session.path()),
              "private",
              uploadState.fileSize,
              false,
              session.tags(),
                  false);

      storageRepository.completeMultipartUpload(fileEntity, uploadState.uploadId, uploadState.eTags);

      return new ChunkedUploadFileResultDto(
          session.path(), uploadState.fileSize, uploadState.totalParts);
    } finally {
      activeUploads.remove(sessionId);
    }
  }

  public void uploadFile(FileUploadDto fileUploadRequest)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    UUID fileId = UUID.randomUUID();
    UUID userId = userSessionService.extractUserIdFromToken(fileUploadRequest.userToken());

    if (storageRepository.fileExists(userId, fileUploadRequest.path()))
      throw new StorageFileAlreadyExistsException(fileUploadRequest.path());

    String mimeType = guessMimeType(fileUploadRequest.path());
    byte[] data = fileUploadRequest.data();

    StorageEntity entity =
        new StorageEntity(
            fileId,
            userId,
            fileUploadRequest.path(),
            mimeType,
            "private",
            data.length,
            false,
            fileUploadRequest.tags(),
            false);

    storageRepository.addFile(entity, data);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationDto fileDownload)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(fileDownload.userToken());

    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, fileDownload.path());
    StorageEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileDownload.path()));

    return new FileDownloadDto(
        fileDownload.path(), entityOpt.get().getMimeType(), storageRepository.downloadFile(entity));
  }

  public void deleteFile(SimpleFileOperationDto deleteFileRequest)
      throws UserNotFoundException, StorageFileNotFoundException, FileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(deleteFileRequest.userToken());
    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, deleteFileRequest.path());
    StorageEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(deleteFileRequest.path()));

    storageRepository.deleteFile(entity);
  }

  public DownloadedChunkDto getFileChunk(GetFileChunkDto fileChunkRequest)
      throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException {
    UUID userId = userSessionService.extractUserIdFromToken(fileChunkRequest.userToken());
    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, fileChunkRequest.filePath());

    StorageEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileChunkRequest.filePath()));

    long chunkSize = fileChunkRequest.chunkSize();
    long offset = fileChunkRequest.chunkIndex() * chunkSize;
    byte[] chunkData =
        storageRepository.downloadFilePart(entity.getUserId(), entity.getEntityId(), offset, chunkSize);

    return new DownloadedChunkDto(
        fileChunkRequest.filePath(), fileChunkRequest.chunkIndex(), chunkData);
  }

  public List<String> getFilePathsList(GetFilePathsListDto filePathsRequest)
      throws UserNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(filePathsRequest.userToken());
    return storageRepository.getFilePathsList(
        userUuid, filePathsRequest.includeDirectories(), filePathsRequest.searchDirectory());
  }

  public FileDto getFileInfo(SimpleFileOperationDto fileInfoRequest)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(fileInfoRequest.userToken());

    Optional<StorageEntity> entityOpt = storageRepository.getFile(userUuid, fileInfoRequest.path());
    if (entityOpt.isEmpty()) throw new StorageFileNotFoundException(fileInfoRequest.path());

    return FileMapper.toDto(entityOpt.get());
  }

  public FileChunkedDownloadDto getFileDownloadInfo(SimpleFileOperationDto fileInfo)
      throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException {
    UUID userUuid = userSessionService.extractUserIdFromToken(fileInfo.userToken());
    Optional<StorageEntity> entityOpt = storageRepository.getFile(userUuid, fileInfo.path());
    StorageEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileInfo.path()));

    return new FileChunkedDownloadDto(
        entity.getEntityId(), fileInfo.path(), entity.getMimeType(), entity.getSize());
  }

  public void changeFileMetadata(ChangeFileMetadataDto changeFileMetadata)
      throws UserNotFoundException,
          StorageFileNotFoundException,
          StorageFileAlreadyExistsException {

    UUID userId = userSessionService.extractUserIdFromToken(changeFileMetadata.userToken());

    Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, changeFileMetadata.oldPath());

    StorageEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(changeFileMetadata.oldPath()));

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
    // TODO: ai ai ai... hardcoding
    byte[] part = combineChunksToPart(uploadState);

    String uploadId = uploadState.getOrCreateUploadId(storageRepository);
    if (part.length > 10 * 1024 * 1024) {
      // TODO: ne tak
      throw new RuntimeException("Chunk size exceeds maximum allowed size");
    }

    String eTag =
        storageRepository.uploadPart(
            uploadId, uploadState.userId, uploadState.fileId, uploadState.partNum, part);
    uploadState.eTags.put(uploadState.partNum, eTag);

    uploadState.totalParts++;
    uploadState.fileSize += part.length;
  }

  // TODO: в другой класс?
  private byte[] combineChunksToPart(ChunkedUploadState upload)
      throws CombineChunksToPartException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      for (byte[] chunk : upload.chunks) {
        outputStream.write(chunk);
      }

      upload.chunks.clear();
      upload.partSize = 0;
      upload.partNum++;

      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new CombineChunksToPartException();
    }
  }

  private static class ChunkedUploadState {

    // TODO: сессия не удаляется, если completeMultipartUpload не вызван
    final FileChunkedUploadDto session;
    final Map<Integer, String> eTags = new HashMap<>();
    final List<byte[]> chunks = new ArrayList<>();
    final UUID userId;
    final UUID fileId;
    final String path;

    String uploadId;
    int fileSize = 0;
    int totalParts = 0;
    int partSize = 0;
    int partNum = 0;

    // TODO: читаемость пупупу

    ChunkedUploadState(FileChunkedUploadDto session, UUID userId, UUID fileId, String path) {
      this.session = session;
      this.userId = userId;
      this.fileId = fileId;
      this.path = path;
    }

    String getOrCreateUploadId(StorageRepository repo) {
      if (uploadId == null) {
        uploadId = repo.startMultipartUpload(userId, fileId);
      }

      return uploadId;
    }
  }

  private String guessMimeType(String filePath) {
    // TODO: вынести в отдельный класс, добавить типов файлов
    if (filePath == null) return "application/octet-stream";
    if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
    if (filePath.endsWith(".png")) return "image/png";
    if (filePath.endsWith(".gif")) return "image/gif";
    if (filePath.endsWith(".pdf")) return "application/pdf";
    if (filePath.endsWith(".txt")) return "text/plain";
    if (filePath.endsWith(".html")) return "text/html";
    if (filePath.endsWith(".mp3")) return "audio/mpeg";
    if (filePath.endsWith(".mp4")) return "video/mp4";
    return "application/octet-stream";
  }
}

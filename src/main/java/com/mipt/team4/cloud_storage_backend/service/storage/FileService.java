package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.FileMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.*;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FileService {
  private final FileRepository fileRepository;
  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();
  private final UserSessionService userSessionService;
  private CompletableFuture<String> uploadId;

  public FileService(FileRepository fileRepository, UserSessionService userSessionService) {
    this.fileRepository = fileRepository;
    this.userSessionService = userSessionService;
  }

  // TODO: soft delete?

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

  public void startChunkedUploadSession(FileChunkedUploadDto uploadSession)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    // TODO: разделить session'ы на юзеровский и файловский
    UUID userId = userSessionService.extractUserIdFromToken(uploadSession.userToken());
    String sessionId = uploadSession.sessionId();
    String path = uploadSession.path();
    if (activeUploads.containsKey(sessionId)) {
      throw new StorageFileAlreadyExistsException(userId, path);
    }

    Optional<FileEntity> fileEntity = fileRepository.getFile(userId, path);
    if (fileEntity.isPresent()) throw new StorageFileAlreadyExistsException(userId, path);

    UUID newFileId = UUID.randomUUID();

    activeUploads.put(
        uploadSession.sessionId(), new ChunkedUploadState(uploadSession, userId, newFileId, path));
  }

  public void uploadChunk(UploadChunkDto chunk) throws UserNotFoundException {
    ChunkedUploadState upload = activeUploads.get(chunk.sessionId());
    if (upload == null) {
      throw new RuntimeException("Upload session not found!");
    }
    CompletableFuture<String> uploadId = upload.getOrCreateUploadId(fileRepository);
    int partNum = chunk.chunkIndex() + 1;
    if (chunk.chunkData().length > 10 * 1024 * 1024) {
      throw new RuntimeException("Chunk size exceeds maximum allowed size");
    }
    CompletableFuture<String> etag =
        fileRepository.uploadPart(
            uploadId, upload.userId, upload.fileId, partNum, chunk.chunkData());
    upload.eTags.put(partNum, etag);

    upload.totalChunks++;
    upload.fileSize += chunk.chunkData().length;
  }

  public ChunkedUploadFileResultDto completeChunkedUpload(String sessionId)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    ChunkedUploadState upload = activeUploads.remove(sessionId);
    if (upload == null) throw new RuntimeException("No such upload session!");

    FileChunkedUploadDto session = upload.session;
    for (int i = 1; i <= upload.totalChunks; i++) {
      if (!upload.eTags.containsKey(i)) throw new RuntimeException("Missing chunk #" + i);
    }

    UUID userId = userSessionService.extractUserIdFromToken(session.userToken());

    String s3Key = StoragePaths.getS3Key(userId, upload.fileId);

    FileEntity fileEntity =
        new FileEntity(
            upload.fileId,
            userId, // TODO: get actualUserId
            s3Key,
            guessMimeType(session.path()),
            "private",
            upload.fileSize,
            false,
            session.tags());

    fileRepository.completeMultipartUpload(fileEntity, upload.uploadId, upload.eTags);

    return new ChunkedUploadFileResultDto(session.path(), upload.fileSize, upload.totalChunks);
  }

  public void uploadFile(FileUploadDto fileUploadRequest)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    UUID fileId = UUID.randomUUID();
    UUID userId = userSessionService.extractUserIdFromToken(fileUploadRequest.userToken());

    if (fileRepository.fileExists(userId, fileUploadRequest.path()))
      throw new StorageFileAlreadyExistsException(userId, fileUploadRequest.path());

    String mimeType = guessMimeType(fileUploadRequest.path());
    byte[] data = fileUploadRequest.data();

    FileEntity entity =
        new FileEntity(
            fileId,
            userId,
            fileUploadRequest.path(),
            mimeType,
            "private",
            data.length,
            false,
            fileUploadRequest.tags());

    // TODO: в FileEntity хранятся и s3Key, и обычный path
    fileRepository.addFile(entity, data);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationDto fileDownload)
      throws StorageIllegalAccessException,
          UserNotFoundException,
          FileNotFoundException,
          StorageFileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(fileDownload.userToken());

    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, fileDownload.path());
    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileDownload.path()));

    return new FileDownloadDto(
        fileDownload.path(), entityOpt.get().getMimeType(), fileRepository.downloadFile(entity));
  }

  public void deleteFile(SimpleFileOperationDto deleteFileRequest)
      throws StorageIllegalAccessException,
          UserNotFoundException,
          StorageFileNotFoundException,
          FileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(deleteFileRequest.userToken());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, deleteFileRequest.path());
    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(deleteFileRequest.path()));

    fileRepository.deleteFile(entity);
  }

  public DownloadedChunkDto getFileChunk(GetFileChunkDto fileChunkRequest)
      throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException {
    UUID userId = userSessionService.extractUserIdFromToken(fileChunkRequest.userToken());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, fileChunkRequest.filePath());

    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileChunkRequest.filePath()));

    byte[] chunkData = fileRepository.downloadFilePart(entity);

    return new DownloadedChunkDto(
        fileChunkRequest.filePath(), fileChunkRequest.chunkIndex(), chunkData);
  }

  public List<String> getFilePathsList(GetFilePathsListDto filePathsRequest)
      throws UserNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(filePathsRequest.userToken());
    return fileRepository.getFilePathsList(userUuid);
  }

  public FileDto getFileInfo(SimpleFileOperationDto fileInfoRequest)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(fileInfoRequest.userToken());

    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, fileInfoRequest.path());
    if (entityOpt.isEmpty()) throw new StorageFileNotFoundException(fileInfoRequest.path());

    return FileMapper.toDto(entityOpt.get());
  }

  public FileChunkedDownloadDto getFileDownloadInfo(SimpleFileOperationDto fileInfo)
      throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException {
    UUID userUuid = userSessionService.extractUserIdFromToken(fileInfo.userToken());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, fileInfo.path());
    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileInfo.path()));

    return new FileChunkedDownloadDto(
        entity.getFileId(), fileInfo.path(), entity.getMimeType(), entity.getSize());
  }

  public void changeFileMetadata(ChangeFileMetadataDto changeFileMetadata)
      throws UserNotFoundException,
          StorageFileNotFoundException,
          StorageFileAlreadyExistsException {

    UUID userId = userSessionService.extractUserIdFromToken(changeFileMetadata.userToken());

    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, changeFileMetadata.oldPath());

    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(changeFileMetadata.oldPath()));

    if (changeFileMetadata.newPath().isPresent()) {
      Optional<FileEntity> existingFile =
          fileRepository.getFile(userId, changeFileMetadata.newPath().get());
      if (existingFile.isPresent()) {
        throw new StorageFileAlreadyExistsException(userId, changeFileMetadata.newPath().get());
      }

      entity.setPath(changeFileMetadata.newPath().get());
    }

    if (changeFileMetadata.tags().isPresent()) {
      entity.setTags(changeFileMetadata.tags().get());
    }

    if (changeFileMetadata.visibility().isPresent()) {
      entity.setVisibility(changeFileMetadata.visibility().get());
    }

    fileRepository.updateFile(entity);
  }

  // TODO: хз как это сделать лучше
  public void createFolder(SimpleFolderOperationDto createFolder) throws UserNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(createFolder.userToken());
    String folderPath = createFolder.folderPath();
    System.out.println("User " + userId + " created folder: " + folderPath);
  }

  public void changeFolderPath(ChangeFolderPathDto changeFolder) {
    // TODO
  }

  public void deleteFolder(SimpleFolderOperationDto request) {
    // TODO
  }

  private static class ChunkedUploadState {
    final FileChunkedUploadDto session;
    final Map<Integer, CompletableFuture<String>> eTags = new HashMap<>();
    final UUID userId;
    final UUID fileId;
    final String path;

    CompletableFuture<String> uploadId;
    int fileSize = 0;
    int totalChunks = 0;

    // TODO: читаемость пупупу

    ChunkedUploadState(FileChunkedUploadDto session, UUID userId, UUID fileId, String path) {
      this.session = session;
      this.userId = userId;
      this.fileId = fileId;
      this.path = path;
    }

    CompletableFuture<String> getOrCreateUploadId(FileRepository repo) {
      if (uploadId == null) {
        uploadId = repo.startMultipartUpload(userId, fileId);
      }
      return uploadId;
    }
  }
}

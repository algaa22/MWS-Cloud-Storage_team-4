package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.FileMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.*;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import com.mipt.team4.cloud_storage_backend.service.user.SessionService;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FileService {
  private final FileRepository fileRepository;
  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();
  private final SessionService sessionService;

  public FileService(FileRepository fileRepository, SessionService sessionService) {
    this.fileRepository = fileRepository;
    this.sessionService = sessionService;
  }

  // TODO: soft delete?

  private String guessMimeType(String filePath) {
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

  public void startChunkedUploadSession(FileChunkedUploadDto session)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    // TODO: разделить session'ы на юзеровский и файловский
    UUID userId = sessionService.extractUserIdFromToken(session.userToken());
    String sessionId = session.sessionId();
    String path = session.path();
    if (activeUploads.containsKey(sessionId)) {
      throw new StorageFileAlreadyExistsException(userId, path);
    }
    Optional<FileEntity> fileEntity = fileRepository.getFile(userId, path);
    if (fileEntity.isPresent()) throw new StorageFileAlreadyExistsException(userId, path);
    activeUploads.put(session.sessionId(), new ChunkedUploadState(session));
  }

  public void processChunk(FileChunkDto chunk) throws UserNotFoundException {
    UUID userId = sessionService.extractUserIdFromToken(chunk.userToken());
    String sessionId = chunk.sessionId();
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
        fileRepository.uploadPart(uploadId, upload.session.path(), partNum, chunk.chunkData());
    upload.eTags.put(partNum, etag);
  }

  public void completeChunkedUpload(String sessionId)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    ChunkedUploadState upload = activeUploads.remove(sessionId);
    if (upload == null) throw new RuntimeException("No such upload session!");

    FileChunkedUploadDto session = upload.session;
    for (int i = 1; i <= session.totalChunks(); i++) {
      if (!upload.eTags.containsKey(i)) throw new RuntimeException("Missing chunk #" + i);
    }

    UUID fileId = UUID.randomUUID();
    UUID userId = sessionService.extractUserIdFromToken(session.userToken());

    String s3Key = StoragePaths.getS3Key(userId, session.path());

    FileEntity fileEntity =
        new FileEntity(
            fileId,
            userId, // TODO: get actualUserId
            s3Key,
            guessMimeType(session.path()),
            "private",
            session.totalFileSize(),
            false,
            session.tags());

    fileRepository.completeMultipartUpload(fileEntity, upload.uploadId, upload.eTags);
  }

  public void uploadFile(FileUploadDto fileUploadRequest)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    UUID fileId = UUID.randomUUID();
    UUID userId = sessionService.extractUserIdFromToken(fileUploadRequest.userToken());

    String s3Key = StoragePaths.getS3Key(userId, fileUploadRequest.path());
    String mimeType = guessMimeType(fileUploadRequest.path());
    byte[] data = fileUploadRequest.data();

    FileEntity entity =
        new FileEntity(
            fileId,
            userId,
            s3Key,
            mimeType,
            "private",
            data.length,
            false,
            fileUploadRequest.tags());

    fileRepository.addFile(entity, data);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationDto fileDownload)
      throws StorageIllegalAccessException,
          UserNotFoundException,
          FileNotFoundException,
          StorageFileNotFoundException {
    UUID userId = sessionService.extractUserIdFromToken(fileDownload.userToken());

    String s3Key = StoragePaths.getS3Key(userId, fileDownload.path());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, s3Key);

    FileEntity entity = entityOpt.orElseThrow(() -> new StorageFileNotFoundException(s3Key));
    checkFileAccess(userId, entity);

    return new FileDownloadDto(
        fileDownload.path(),
        entityOpt.get().getMimeType(),
        fileRepository.downloadFile(entity.getS3Key()));
  }

  public void deleteFile(SimpleFileOperationDto deleteFileRequest)
      throws StorageIllegalAccessException,
          UserNotFoundException,
          StorageFileNotFoundException,
          FileNotFoundException {
    UUID userUuid = sessionService.extractUserIdFromToken(deleteFileRequest.userToken());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, deleteFileRequest.path());
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    checkFileAccess(userUuid, entity);
    fileRepository.deleteFile(userUuid, deleteFileRequest.path());
  }

  private void checkFileAccess(UUID userId, FileEntity entity)
      throws StorageIllegalAccessException {
    if (!entity.getOwnerId().equals(userId)) {
      throw new StorageIllegalAccessException();
    }
  }


  public FileChunkDto getFileChunk(GetFileChunkDto fileChunkRequest)
      throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException {
    UUID userId = sessionService.extractUserIdFromToken(fileChunkRequest.userToken());
    String s3Key = StoragePaths.getS3Key(userId, fileChunkRequest.fileId());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, s3Key);

    FileEntity entity = entityOpt.orElseThrow(() ->
        new StorageFileNotFoundException(fileChunkRequest.fileId()));
    checkFileAccess(userId, entity);
    long fileSize = entity.getSize();
    long chunkSize = fileChunkRequest.chunkSize();
    long chunkIndex = fileChunkRequest.chunkIndex();
    long offset = chunkIndex * chunkSize;
    if (offset >= fileSize) {
      throw new RuntimeException("Chunk index out of bounds");
    }

    long actualChunkSize = Math.min(chunkSize, fileSize - offset);
    byte[] chunkData = fileRepository.downloadFilePart(entity.getS3Key(), offset, actualChunkSize);

    return new FileChunkDto(
        fileChunkRequest.sessionId(),
        fileChunkRequest.fileId(),
        fileChunkRequest.chunkIndex(),
        fileChunkRequest.chunkData(),
        fileChunkRequest.userToken(),
        chunkIndex * chunkSize + actualChunkSize >= fileSize
    );
  }

  public List<String> getFilePathsList(GetFilePathsListDto filePathsRequest)
      throws UserNotFoundException {
    UUID userUuid = sessionService.extractUserIdFromToken(filePathsRequest.userToken());
    return fileRepository.getFilePathsList(userUuid);
  }

  public FileDto getFileInfo(SimpleFileOperationDto fileInfoRequest)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userUuid = sessionService.extractUserIdFromToken(fileInfoRequest.userToken());
    String s3Key = StoragePaths.getS3Key(userUuid, fileInfoRequest.path());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, s3Key);
    if (entityOpt.isEmpty()) throw new StorageFileNotFoundException(fileInfoRequest.path());
    return FileMapper.toDto(entityOpt.get());
  }

  public FileChunkedDownloadDto getFileDownloadInfo(SimpleFileOperationDto fileInfo)
      throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException {
    UUID userUuid = sessionService.extractUserIdFromToken(fileInfo.userToken());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, fileInfo.path());
    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileInfo.path()));
    checkFileAccess(userUuid, entity);

    return new FileChunkedDownloadDto(
        entity.getFileId(), fileInfo.path(), entity.getMimeType(), entity.getSize());
  }

  public void changeFileMetadata(ChangeFileMetadataDto changeFileMetadata)
      throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException, StorageFileAlreadyExistsException {

    UUID userId = sessionService.extractUserIdFromToken(changeFileMetadata.userToken());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, changeFileMetadata.oldPath());

    FileEntity entity = entityOpt.orElseThrow(() ->
        new StorageFileNotFoundException(changeFileMetadata.oldPath()));
    checkFileAccess(userId, entity);

    if (changeFileMetadata.newPath() != null) {
      Optional<FileEntity> existingFile = fileRepository.getFile(userId,
          String.valueOf(changeFileMetadata.newPath()));
      if (existingFile.isPresent()) {
        throw new StorageFileAlreadyExistsException(userId, String.valueOf(changeFileMetadata.newPath()));
      }
      entity.setS3Key(StoragePaths.getS3Key(userId, String.valueOf(changeFileMetadata.newPath())));
    }

    if (changeFileMetadata.tags() != null) {
      entity.setTags(changeFileMetadata.tags());
    }

    if (changeFileMetadata.visibility() != null) {
      entity.setVisibility(String.valueOf(changeFileMetadata.visibility()));
    }

    fileRepository.updateFile(entity);
  }

  //TODO: хз как это сделать лучше
  public void createFolder(SimpleFolderOperationDto createFolder)
      throws UserNotFoundException {
    UUID userId = sessionService.extractUserIdFromToken(createFolder.userToken());
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
    CompletableFuture<String> uploadId;

    ChunkedUploadState(FileChunkedUploadDto session) {
      this.session = session;
    }

    CompletableFuture<String> getOrCreateUploadId(FileRepository repo) {
      if (uploadId == null) {
        String s3Key = session.userToken() + "/" + session.path();
        uploadId = repo.startMultipartUpload(s3Key);
      }
      return uploadId;
    }
  }
}

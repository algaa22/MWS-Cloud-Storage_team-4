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

  private static String guessMimeType(String path) {
    // TODO: peredelat :))))))))))
    if (path.endsWith(".jpg")) return "image/jpeg";
    if (path.endsWith(".png")) return "image/png";
    if (path.endsWith(".pdf")) return "application/pdf";
    return "application/octet-stream";
  }

  public void startChunkedUploadSession(FileChunkedUploadDto session)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    // TODO: разделить session'ы на юзеровский и файловский
    UUID userId = sessionService.extractUserIdFromToken(session.userToken());

    // TODO: если с таким session.sessionId() уже есть сессия в activeUploads?

    String path = session.path();
    Optional<FileEntity> fileEntity = fileRepository.getFile(userId, path);

    if (fileEntity.isPresent()) throw new StorageFileAlreadyExistsException(userId, path);

    activeUploads.put(session.sessionId(), new ChunkedUploadState(session));
  }

  public void processChunk(FileChunkDto chunk) {
    ChunkedUploadState upload = activeUploads.get(chunk.sessionId());
    if (upload == null) throw new RuntimeException("Upload session not found!");
    CompletableFuture<String> uploadId = upload.getOrCreateUploadId(fileRepository);
    int partNum = chunk.chunkIndex() + 1;
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
      throws StorageIllegalAccessException, UserNotFoundException, FileNotFoundException {
    UUID userId = sessionService.extractUserIdFromToken(fileDownload.userToken());

    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, fileDownload.path());
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
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
    // TODO: а нужен ли?
    if (!entity.getOwnerId().equals(userId)) {
      throw new StorageIllegalAccessException();
    }
  }

  // TODO: переделать
  public FileChunkDto getFileChunk(GetFileChunkDto fileChunkRequest) {
    return null;
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

  public void changeFileMetadata(ChangeFileMetadataDto changeFileMetadata) {
    // TODO
  }

  public void createFolder(SimpleFolderOperationDto createFolder) {
    // TODO
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

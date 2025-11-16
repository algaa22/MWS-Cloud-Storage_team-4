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
import java.io.InputStream;
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

  private static String guessMimeType(String path) {
    // TODO: peredelat :))))))))))
    if (path.endsWith(".jpg")) return "image/jpeg";
    if (path.endsWith(".png")) return "image/png";
    if (path.endsWith(".pdf")) return "application/pdf";
    return "application/octet-stream";
  }

  public void startChunkedUploadSession(FileChunkedUploadDto session) throws UserNotFoundException, StorageFileAlreadyExistsException {
    // TODO: разделить session'ы на юзеровский и файловский
    UUID userId = sessionService.extractUserIdFromToken(session.userToken());

    // TODO: если с таким session.sessionId() уже есть сессия в activeUploads?

    String path = session.path();
    Optional<FileEntity> fileEntity = fileRepository.getFile(userId, path);

    if (fileEntity.isPresent())
      throw new StorageFileAlreadyExistsException(userId, path);

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

  public void completeChunkedUpload(String sessionId) throws StorageFileAlreadyExistsException, UserNotFoundException {
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

  public void uploadFile(FileUploadDto fileUploadRequest) throws StorageFileAlreadyExistsException, UserNotFoundException {
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

  public InputStream downloadFile(String userToken, String path) throws StorageIllegalAccessException, UserNotFoundException {
    UUID userId = sessionService.extractUserIdFromToken(userToken);

    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    checkFileAccess(userId, entity);

    return fileRepository.downloadFile(entity.getS3Key());
  }

  public void deleteFile(String userToken, String path) throws StorageIllegalAccessException, UserNotFoundException, StorageFileNotFoundException, FileNotFoundException {
    UUID userUuid = sessionService.extractUserIdFromToken(userToken);
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    checkFileAccess(userUuid, entity);
    fileRepository.deleteFile(userUuid, path);
  }

  private void checkFileAccess(UUID userId, FileEntity entity)
      throws StorageIllegalAccessException {
    // TODO: а нужен ли?
    if (!entity.getOwnerId().equals(userId)) {
      throw new StorageIllegalAccessException();
    }
  }

  //TODO: переделать
  public FileChunkDto getFileChunk(String fileId, int chunkIndex, int chunkSize) throws UserNotFoundException, StorageIllegalAccessException, StorageFileNotFoundException {
    return null;
  }

  public List<String> getFilePathsList(String token) throws UserNotFoundException {
    UUID userUuid = sessionService.extractUserIdFromToken(token);
    return fileRepository.getFilePathsList(userUuid);
  }

  public FileDto getFileInfo(String token, String path) throws UserNotFoundException, StorageFileNotFoundException {
    UUID userUuid = sessionService.extractUserIdFromToken(token);
    String s3Key = StoragePaths.getS3Key(userUuid, path);
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, s3Key);
    if(entityOpt.isEmpty()) throw new StorageFileNotFoundException(path);
    return FileMapper.toDto(entityOpt.get());
  }

  public FileChunkedDownloadDto getFileDownloadInfo(String userToken, String path) throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException {
    UUID userUuid = sessionService.extractUserIdFromToken(userToken);
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new StorageFileNotFoundException(path));
    checkFileAccess(userUuid, entity);
    int chunkSize = 2 * 1024 * 1024; //TODO: какой по умолчанию?
    int chunks = (int) Math.ceil(entity.getSize() / (double) chunkSize);
    return new FileChunkedDownloadDto(entity.getFileId(), path, entity.getMimeType(), entity.getSize());
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

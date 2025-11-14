package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.FileMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedDownloadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FileService {
  private final FileRepository fileRepository;
  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();

  public FileService(FileRepository fileRepository) {
    this.fileRepository = fileRepository;
  }

  // Для chunked upload

  public void startChunkedUploadSession(FileChunkedUploadDto session)
      throws StorageIllegalAccessException {
    UUID ownerUuid = UUID.fromString(session.ownerId());
    // TODO: actualUserId получать из репозитория
    UUID realUserUuid = UUID.fromString(null);

    if (!ownerUuid.equals(realUserUuid)) {
      throw new StorageIllegalAccessException();
    }

    String path = session.path();
    Optional<FileEntity> existing = fileRepository.getFile(ownerUuid, path);
    if (existing.isPresent()) {
      throw new RuntimeException("File already exists with this ownerId and path.");
    }
    activeUploads.put(session.sessionId(), new ChunkedUploadState(session));
  }

  public void processChunk(FileChunkDto chunk) {
    ChunkedUploadState upload = activeUploads.get(chunk.sessionId());
    if (upload == null) throw new RuntimeException("Upload session not found!");
    CompletableFuture<String> uploadId = upload.getOrCreateUploadId(fileRepository);
    int partNum = chunk.chunkIndex() + 1;
    CompletableFuture<String> etag = fileRepository.uploadPart(uploadId, upload.session.path(), partNum, chunk.chunkData());
    upload.etags.put(partNum, etag);
  }

  public void completeChunkedUpload(String sessionId) throws StorageFileAlreadyExistsException {
    ChunkedUploadState upload = activeUploads.remove(sessionId);
    if (upload == null) throw new RuntimeException("No such upload session!");

    FileChunkedUploadDto session = upload.session;
    String s3Key = session.ownerId() + "/" + session.path();
    for (int i = 1; i <= session.totalChunks(); i++) {
      if (!upload.etags.containsKey(i)) throw new RuntimeException("Missing chunk #" + i);
    }

    fileRepository.completeMultipartUpload(s3Key, upload.uploadId, upload.etags);

    UUID fileId = UUID.randomUUID();
    FileEntity entity =
        new FileEntity(
            fileId,
            null, // TODO: get actualUserId
            s3Key,
            guessMimeType(session.path()),
            "private",
            session.totalFileSize(),
            false,
            session.tags());
    fileRepository.addFile(entity);
  }

  // Обычная загрузка файла

  public FileDto uploadFile(
      String ownerId,
      String fileName,
      InputStream stream,
      String contentType,
      long size,
      List<String> tags,
      String actualUserId)
      throws StorageIllegalAccessException, StorageFileAlreadyExistsException {
    UUID fileId = UUID.randomUUID();
    UUID realUserUuid = UUID.fromString(actualUserId);
    UUID pathOwnerUuid = UUID.fromString(ownerId);

    if (!realUserUuid.equals(pathOwnerUuid)) throw new StorageIllegalAccessException();

    String s3Key = ownerId + "/" + fileName;
    fileRepository.putObject(s3Key, stream, contentType);

    FileEntity entity =
        new FileEntity(fileId, pathOwnerUuid, s3Key, contentType, "private", size, false, tags);
    fileRepository.addFile(entity);
    return FileMapper.toDto(entity);
  }

  // Скачивание файла (только если userId == ownerId)

  public InputStream downloadFile(String userId, String path) throws StorageIllegalAccessException {
    UUID userUuid = UUID.fromString(userId);
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    checkFileAccess(userUuid, entity);
    return fileRepository.downloadObject(entity.getStoragePath());
  }

  public void deleteFile(String userId, String path) throws StorageIllegalAccessException {
    UUID userUuid = UUID.fromString(userId);
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    checkFileAccess(userUuid, entity);
    // TODO: реализовать логику soft delete в репозитории
  }

  private void checkFileAccess(UUID userId, FileEntity entity)
      throws StorageIllegalAccessException {
    if (!entity.getOwnerId().equals(userId)) {
      throw new StorageIllegalAccessException();
    }
  }

  private static String guessMimeType(String path) {
    // TODO: peredelat :))))))))))
    if (path.endsWith(".jpg")) return "image/jpeg";
    if (path.endsWith(".png")) return "image/png";
    if (path.endsWith(".pdf")) return "application/pdf";
    return "application/octet-stream";
  }

  public FileChunkDto getFileChunk(String fileId, int chunkINdex, int i1) {
    return null;
  }

  public List<String> getFilePathsList(String s) {
    return null;
  }

  public FileDto getFileInfo(String s, String s1) {
    return null;
  }

  public FileChunkedDownloadDto getFileDownloadInfo(String s, String s1) {
    return null;
  }

  // multipart upload state
  private static class ChunkedUploadState {
    final FileChunkedUploadDto session;
    final Map<Integer, CompletableFuture<String>> etags = new HashMap<>();
    CompletableFuture<String> uploadId;

    ChunkedUploadState(FileChunkedUploadDto session) {
      this.session = session;
    }

    CompletableFuture<String> getOrCreateUploadId(FileRepository repo) {
      if (uploadId == null) {
        String s3Key = session.ownerId() + "/" + session.path();
        uploadId = repo.startMultipartUpload(s3Key);
      }
      return uploadId;
    }
  }
}

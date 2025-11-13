package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.model.storage.FileMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileContentRepository;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileService {
  private final FileRepository fileRepository;
  private final FileContentRepository contentRepository;
  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();

  public FileService(FileRepository fileRepository, FileContentRepository contentRepository) {
    this.fileRepository = fileRepository;
    this.contentRepository = contentRepository;
  }

  // Проверка, что userId == ownerId (или админ). Можно расширить на роли!
  private void checkFileAccess(UUID userId, FileEntity entity) {
    if (!entity.getOwnerId().equals(userId)) {
      throw new StorageIllegalAccessException("Access denied: user is not file owner.");
    }
  }

  // Для chunked upload
  public void startChunkedUploadSession(FileChunkedUploadDto session, String actualUserId)
      throws DbExecuteQueryException {
    UUID ownerUuid = UUID.fromString(session.ownerId());
    UUID realUserUuid = UUID.fromString(actualUserId);
    if (!ownerUuid.equals(realUserUuid)) {
      throw new StorageIllegalAccessException("Access denied: cannot upload for another user.");
    }
    String path = session.path();
    Optional<FileEntity> existing = fileRepository.getFile(ownerUuid, path);
    if (existing.isPresent()) {
      throw new RuntimeException("File already exists with this ownerId and path.");
    }
    activeUploads.put(session.sessionId(), new ChunkedUploadState(session));
  }

  public void processChunk(FileChunkDto chunk, UUID actualUserId) throws DbExecuteQueryException {
    ChunkedUploadState upload = activeUploads.get(chunk.sessionId());
    if (upload == null) throw new RuntimeException("Upload session not found!");
    String uploadId = upload.getOrCreateUploadId(contentRepository);
    int partNum = chunk.chunkIndex() + 1;
    String etag = contentRepository.uploadPart(uploadId, partNum, chunk.chunkData());
    upload.etags.put(partNum, etag);
  }

  public UUID finishChunkedUpload(String sessionId, UUID actualUserId)
      throws DbExecuteUpdateException, DbExecuteQueryException {
    ChunkedUploadState upload = activeUploads.remove(sessionId);
    if (upload == null) throw new RuntimeException("No such upload session!");

    FileChunkedUploadDto session = upload.session;
    String s3Key = session.ownerId() + "/" + session.path();
    for (int i = 1; i <= session.totalChunks(); i++) {
      if (!upload.etags.containsKey(i))
        throw new RuntimeException("Missing chunk #" + i);
    }
    List<String> etagList = new ArrayList<>();
    for (int i = 1; i <= session.totalChunks(); i++) {
      etagList.add(upload.etags.get(i));
    }
    contentRepository.completeMultipartUpload(s3Key, upload.uploadId, etagList);

    UUID fileId = UUID.randomUUID();
    FileEntity entity = new FileEntity(
        fileId,
        actualUserId,
        s3Key,
        guessMimeType(session.path()),
        "private",
        session.totalFileSize(),
        false,
        session.tags()
    );
    fileRepository.addFile(entity);
    return fileId;
  }

  // Обычная загрузка файла
  public FileDto uploadFile(String ownerId, String fileName, InputStream stream, String contentType, long size, List<String> tags, String actualUserId)
      throws DbExecuteUpdateException {
    UUID fileId = UUID.randomUUID();
    UUID realUserUuid = UUID.fromString(actualUserId);
    UUID pathOwnerUuid = UUID.fromString(ownerId);
    if (!realUserUuid.equals(pathOwnerUuid)) {
      throw new StorageIllegalAccessException("Access denied: cannot upload for another user.");
    }
    String s3Key = ownerId + "/" + fileName;
    contentRepository.putObject(s3Key, stream, contentType);

    FileEntity entity = new FileEntity(
        fileId,
        pathOwnerUuid,
        s3Key,
        contentType,
        "private",
        size,
        false,
        tags
    );
    fileRepository.addFile(entity);
    return FileMapper.toDto(entity);
  }

  // Скачивание файла (только если userId == ownerId)
  public InputStream downloadFile(String userId, String path) throws DbExecuteQueryException {
    UUID userUuid = UUID.fromString(userId);
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    checkFileAccess(userUuid, entity);
    return contentRepository.downloadObject(entity.getStoragePath());
  }

  public void deleteFile(String userId, String path) throws DbExecuteQueryException {
    UUID userUuid = UUID.fromString(userId);
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    checkFileAccess(userUuid, entity);
    // TODO: реализовать логику soft delete в репозитории
  }

  private static String guessMimeType(String path) {
    if (path.endsWith(".jpg")) return "image/jpeg";
    if (path.endsWith(".png")) return "image/png";
    if (path.endsWith(".pdf")) return "application/pdf";
    return "application/octet-stream";
  }

  // multipart upload state
  private static class ChunkedUploadState {
    final FileChunkedUploadDto session;
    final Map<Integer, String> etags = new HashMap<>();
    String uploadId;

    ChunkedUploadState(FileChunkedUploadDto session) {
      this.session = session;
    }

    String getOrCreateUploadId(FileContentRepository repo) {
      if (uploadId == null) {
        String s3Key = session.ownerId() + "/" + session.path();
        uploadId = repo.startMultipartUpload(s3Key);
      }
      return uploadId;
    }
  }
}

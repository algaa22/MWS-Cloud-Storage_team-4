package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.model.storage.FileMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunk;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedDownloadInfo;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileInfo;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileContentRepository;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileService {
  private final FileRepository fileRepository;
  // Карту держим только для uploadId и эритагов — сервис хранит техническую multipart сессию
  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();

  public FileService(FileRepository fileRepository) {
    this.fileRepository = fileRepository;
  }

  // Инициализация chunked-upload: никаких записьей в репо, только запоминаем сессию
  public void startChunkedUploadSession(FileChunkedUploadSession session)
      throws DbExecuteQueryException {
    UUID ownerUuid = UUID.fromString(session.ownerId());
    String path = session.path();
    Optional<FileEntity> existing = fileRepository.getFile(ownerUuid, path);
    if (existing.isPresent()) {
      throw new RuntimeException("File already exists with this ownerId and path.");
    }
    activeUploads.put(session.sessionId(), new ChunkedUploadState(session));
  }

  // Обработка чанка — upload в MinIO и запоминание etag
  public void processChunk(FileChunk chunk) {
    ChunkedUploadState upload = activeUploads.get(chunk.sessionId());
    if (upload == null) throw new RuntimeException("Upload session not found!");

    String uploadId = upload.getOrCreateUploadId(contentRepository);
    int partNum = chunk.chunkIndex() + 1; // MinIO/S3 parts с 1
    String etag = contentRepository.uploadPart(uploadId, partNum, chunk.chunkData());
    upload.etags.put(partNum, etag);
  }

  // Финализация multipart-upload, создание записи о файле
  public UUID finishChunkedUpload(String sessionId) throws DbExecuteUpdateException {
    ChunkedUploadState upload = activeUploads.remove(sessionId);
    if (upload == null) throw new RuntimeException("No such upload session!");

    FileChunkedUploadSession session = upload.session;
    String s3Key = session.ownerId() + "/" + session.path();

    // Проверка completeness частей
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
        UUID.fromString(session.ownerId()),
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

  // Прямая (обычная) загрузка файла
  public FileInfo uploadFile(String ownerId, String fileName, InputStream stream, String contentType, long size, List<String> tags)
      throws DbExecuteUpdateException {
    UUID fileId = UUID.randomUUID();
    String s3Key = ownerId + "/" + fileName;
    contentRepository.putObject(s3Key, stream, contentType);

    FileEntity entity = new FileEntity(
        fileId.toString(),
        ownerId,
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

  public InputStream downloadFile(String ownerId, String path) throws DbExecuteQueryException {
    UUID ownerUuid = UUID.fromString(ownerId);
    Optional<FileEntity> entityOpt = fileRepository.getFile(ownerUuid, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    return contentRepository.downloadObject(entity.getStoragePath());
  }

  // Soft delete (если появится метод в FileRepository)
  public void deleteFile(String ownerId, String path) throws DbExecuteQueryException {
    UUID ownerUuid = UUID.fromString(ownerId);
    Optional<FileEntity> entityOpt = fileRepository.getFile(ownerUuid, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    // TODO: here add/update logic for soft-deleting in repo when implemented
  }

  private static String guessMimeType(String path) { // TODO: guess?????????
    if (path.endsWith(".jpg")) return "image/jpeg";
    if (path.endsWith(".png")) return "image/png";
    if (path.endsWith(".pdf")) return "application/pdf";
    return "application/octet-stream";
  }

  public FileChunkedDownloadInfo getFileDownloadInfo(String s, String s1) {

  }

  public FileChunk getFileChunk(String currentFileId, int chunkIndex, int chunkSize) {

  }

  public List<String> getFilePathsList(String userId) {

  }

  public FileInfo getFileInfo(String fileId, String userId) {

  }

  // --- multipart upload state ---
  private static class ChunkedUploadState {

    final FileChunkedUploadSession session;
    final Map<Integer, String> etags = new HashMap<>(); // part -> etag
    String uploadId;

    ChunkedUploadState(FileChunkedUploadSession session) {
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

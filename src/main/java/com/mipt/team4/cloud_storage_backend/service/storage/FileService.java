package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.exception.service.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.service.TranferSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.FileChunkedUploadMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.FileMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedDownloadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileChunkedUploadEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileService {
  private final FileRepository fileRepository;
  private Map<UUID, FileChunkedUploadEntity> chunkedUploadSessions = new ConcurrentHashMap<>();

  public FileService(FileRepository fileRepository) {
    this.fileRepository = fileRepository;
  }

  public void startChunkedUploadSession(FileChunkedUploadDto chunkedUploadDto)
      throws StorageFileAlreadyExistsException {
    // TODO: Проверка на квоты
    UUID ownerId = UUID.fromString(chunkedUploadDto.ownerId());
    UUID sessionId = UUID.fromString(chunkedUploadDto.sessionId());
    String path = chunkedUploadDto.path();

    if (fileRepository.fileExists(ownerId, path))
      throw new StorageFileAlreadyExistsException(ownerId, path);

    chunkedUploadSessions.put(sessionId, createChunkedUploadSession(chunkedUploadDto));
  }

  public void processChunk(FileChunkDto chunk) throws TranferSessionNotFoundException {
    UUID sessionId = UUID.fromString(chunk.sessionId());

    FileChunkedUploadEntity session = chunkedUploadSessions.get(sessionId);
    if (session == null) throw new TranferSessionNotFoundException(sessionId);

    String uploadId = session.getS3UploadId();
    int partIndex = StoragePaths.toS3PartIndex(chunk.chunkIndex());
    String eTag = fileRepository.uploadPart(uploadId, partIndex, chunk.chunkData());

    session.putETag(partIndex, eTag);
  }

  public UUID finishChunkedUpload(String sessionId)
      throws TranferSessionNotFoundException, MissingFilePartException {
    FileChunkedUploadEntity session = chunkedUploadSessions.get(UUID.fromString(sessionId));
    if (session == null) throw new TranferSessionNotFoundException(sessionId);

    String s3Key = StoragePaths.getS3Key(session.getOwnerId(), session.getPath());
    List<String> etagList = new ArrayList<>();

    for (int partIndex = 1; partIndex <= session.getTotalChunks(); partIndex++) {
      if (session.getETag(partIndex) == null) throw new MissingFilePartException(partIndex);

      etagList.add(session.getETag(partIndex));
    }

    return fileRepository.finishMultipartUpload(s3Key, session.getS3UploadId(), etagList);
  }

  // Прямая (обычная) загрузка файла
  public FileDto uploadFile(
      String ownerId,
      String fileName,
      InputStream stream,
      String contentType,
      long size,
      List<String> tags)
      throws DbExecuteUpdateException {
    UUID fileId = UUID.randomUUID();
    String s3Key = ownerId + "/" + fileName;
    //contentRepository.putObject(s3Key, stream, contentType);

    FileEntity entity =
        new FileEntity(fileId, null, s3Key, contentType, "private", size, false, tags);
    fileRepository.addFile(entity);
    return FileMapper.toDto(entity);
  }

  public InputStream downloadFile(String ownerId, String path) throws DbExecuteQueryException {
    UUID ownerUuid = UUID.fromString(ownerId);
    Optional<FileEntity> entityOpt = fileRepository.getFile(ownerUuid, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    return null;
  }

  // Soft delete (если появится метод в FileRepository)
  public void deleteFile(String ownerId, String path) throws DbExecuteQueryException {
    UUID ownerUuid = UUID.fromString(ownerId);
    Optional<FileEntity> entityOpt = fileRepository.getFile(ownerUuid, path);
    FileEntity entity = entityOpt.orElseThrow(() -> new RuntimeException("File not found"));
    // TODO: here add/update logic for soft-deleting in repo when implemented
  }

  public FileChunkedDownloadDto getFileDownloadInfo(String fileId, String userId) {
    return null;
  }

  public FileChunkDto getFileChunk(String fileId, int chunkIndex, int chunkSize) {
    return null;
  }

  public List<String> getFilePathsList(String userId) {
    return null;
  }

  public FileDto getFileInfo(String fileId, String userId) {
    return null;
  }

  private FileChunkedUploadEntity createChunkedUploadSession(
          FileChunkedUploadDto chunkedUploadDto) {
    FileChunkedUploadEntity session = FileChunkedUploadMapper.toEntity(chunkedUploadDto);

    String fullPath = StoragePaths.getS3Key(session.getOwnerId(), session.getPath());
    session.setS3UploadId(fileRepository.startMultipartUpload(fullPath));

    return session;
  }

  private static String guessMimeType(String path) {
    if (path.endsWith(".jpg")) return "image/jpeg";
    if (path.endsWith(".png")) return "image/png";
    if (path.endsWith(".pdf")) return "application/pdf";
    return "application/octet-stream";
  }
}

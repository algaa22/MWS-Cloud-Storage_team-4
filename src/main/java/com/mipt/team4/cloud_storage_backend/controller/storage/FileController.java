package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileUploadValidateException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileController {
  private static final Logger logger = LoggerFactory.getLogger(FileController.class);

  private final FileService service;
  private final StorageConfig storageConfig;

  private Map<String, FileChunkedUploadSession> chunkedUploadSessions;

  public FileController(FileService service, StorageConfig storageConfig) {
    this.service = service;
    this.storageConfig = storageConfig;
  }

  public void startChunkedUpload(FileChunkedUploadSession chunkedUploadSession)
      throws FileUploadValidateException, DbExecuteQueryException {
    validateChunkedUploadDto(chunkedUploadSession);
    service.startChunkedUploadSession(chunkedUploadSession);
    chunkedUploadSessions.put(chunkedUploadSession.sessionId(), chunkedUploadSession);
  }

  public void processFileChunk(FileChunkDto fileChunkDto) throws FileUploadValidateException {
    validateChunkData(fileChunkDto);
    service.processChunk(fileChunkDto);
  }

  public UUID finishChunkedUpload(String currentSessionId) {
    FileChunkedUploadSession chunkedUploadSession = chunkedUploadSessions.get(currentSessionId);
    chunkedUploadSessions.remove(currentSessionId);

    return service.finishChunkedUpload(currentSessionId);
  }

  private void validateChunkedUploadDto(FileChunkedUploadSession chunkedUploadSession)
      throws FileUploadValidateException {
    if (chunkedUploadSession.sessionId() == null
        || chunkedUploadSession.sessionId().trim().isEmpty())
      throw new FileUploadValidateException("Session ID is required");

    if (chunkedUploadSession.ownerId() == null || chunkedUploadSession.ownerId().trim().isEmpty())
      throw new FileUploadValidateException("Owner ID is required");

    if (chunkedUploadSession.filePath() == null || chunkedUploadSession.filePath().trim().isEmpty())
      throw new FileUploadValidateException("File path is required");

    if (chunkedUploadSession.totalFileSize() < 0)
      throw new FileUploadValidateException("File size cannot be negative");

    if (chunkedUploadSession.totalChunks() <= 0)
      throw new FileUploadValidateException("Total chunks must be positive");

    if (chunkedUploadSession.totalFileSize() > storageConfig.getMaxFileSize())
      throw new FileUploadValidateException("File size exceeds maximum allowed limit");
  }

  private void validateChunkData(FileChunkDto fileChunkDto) throws FileUploadValidateException {
    if (fileChunkDto.sessionId() == null || fileChunkDto.sessionId().trim().isEmpty())
      throw new FileUploadValidateException("Session ID is required");

    if (fileChunkDto.chunkIndex() < 0)
      throw new FileUploadValidateException("Chunk index cannot be negative");

    if (fileChunkDto.chunkData() == null)
      throw new FileUploadValidateException("Chunk data cannot be null");

    if (fileChunkDto.chunkData().length == 0)
      throw new FileUploadValidateException("Chunk data cannot be empty");

    if (fileChunkDto.chunkData().length > storageConfig.getMaxFileChunkSize())
      throw new FileUploadValidateException("Chunk size exceeds maximum allowed limit");
  }
}

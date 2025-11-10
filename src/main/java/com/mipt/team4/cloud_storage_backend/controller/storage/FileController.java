package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.http.validation.FileDownloadValidationException;
import com.mipt.team4.cloud_storage_backend.exception.http.validation.FileUploadValidationException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunk;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfo;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileController {
  private static final Logger logger = LoggerFactory.getLogger(FileController.class);

  private final FileService service;

  private Map<String, FileChunkedUploadSession> chunkedUploadSessions;

  public FileController(FileService service) {
    this.service = service;
  }

  public void startChunkedUpload(FileChunkedUploadSession chunkedUploadSession)
      throws FileUploadValidationException {
    validateChunkedUploadDto(chunkedUploadSession);
    service.startChunkedUploadSession(chunkedUploadSession);
    chunkedUploadSessions.put(chunkedUploadSession.sessionId(), chunkedUploadSession);
  }

  public void processFileChunk(FileChunk fileChunk) throws FileUploadValidationException {
    validateChunkData(fileChunk);
    service.processChunk(fileChunk);
  }

  public String finishChunkedUpload(String sessionId) {
    FileChunkedUploadSession chunkedUploadSession = chunkedUploadSessions.get(sessionId);
    chunkedUploadSessions.remove(sessionId);

    return service.finishChunkedUpload(chunkedUploadSession);
  }

  public FileDownloadInfo getFileDownloadInfo(String fileId, String userId) throws FileDownloadValidationException {
    validateChunkedDownloadInfo(fileId, userId);
    return service.getFileDownloadInfo(fileId, userId);
  }

  public FileChunk getFileChunk(String currentFileId, int chunkIndex, int chunkSize) {
    return service.getFileChunk(currentFileId, chunkIndex, chunkSize);
  }

  private void validateChunkedDownloadInfo(String fileId, String userId) throws FileDownloadValidationException {
    if (fileId == null || fileId.trim().isEmpty())
      throw new FileDownloadValidationException("File ID is required");

    if (userId == null || userId.trim().isEmpty())
      throw new FileDownloadValidationException("User ID is required");
  }

  private void validateChunkedUploadDto(FileChunkedUploadSession chunkedUploadSession)
      throws FileUploadValidationException {
    if (chunkedUploadSession.sessionId() == null
        || chunkedUploadSession.sessionId().trim().isEmpty())
      throw new FileUploadValidationException("Session ID is required");

    if (chunkedUploadSession.ownerId() == null || chunkedUploadSession.ownerId().trim().isEmpty())
      throw new FileUploadValidationException("Owner ID is required");

    if (chunkedUploadSession.path() == null || chunkedUploadSession.path().trim().isEmpty())
      throw new FileUploadValidationException("File path is required");

    if (chunkedUploadSession.totalFileSize() < 0)
      throw new FileUploadValidationException("File size cannot be negative");

    if (chunkedUploadSession.totalChunks() <= 0)
      throw new FileUploadValidationException("Total chunks must be positive");

    if (chunkedUploadSession.totalFileSize() > StorageConfig.getInstance().getMaxFileSize())
      throw new FileUploadValidationException("File size exceeds maximum allowed limit");
  }

  private void validateChunkData(FileChunk fileChunk) throws FileUploadValidationException {
    if (fileChunk.sessionId() == null || fileChunk.sessionId().trim().isEmpty())
      throw new FileUploadValidationException("Session ID is required");

    if (fileChunk.chunkIndex() < 0)
      throw new FileUploadValidationException("Chunk index cannot be negative");

    if (fileChunk.chunkData() == null)
      throw new FileUploadValidationException("Chunk data cannot be null");

    if (fileChunk.chunkData().length == 0)
      throw new FileUploadValidationException("Chunk data cannot be empty");

    if (fileChunk.chunkData().length > StorageConfig.getInstance().getMaxFileChunkSize())
      throw new FileUploadValidationException("Chunk size exceeds maximum allowed limit");
  }
}

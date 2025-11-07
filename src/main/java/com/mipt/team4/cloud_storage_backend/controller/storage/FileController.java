package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileDownloadValidateException;
import com.mipt.team4.cloud_storage_backend.exception.storage.FileUploadValidateException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunk;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadInfo;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import java.util.Map;
import java.util.UUID;

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
      throws FileUploadValidateException {
    validateChunkedUploadDto(chunkedUploadSession);
    service.startChunkedUploadSession(chunkedUploadSession);
    chunkedUploadSessions.put(chunkedUploadSession.sessionId(), chunkedUploadSession);
  }

  public void processFileChunk(FileChunk fileChunk) throws FileUploadValidateException {
    validateChunkData(fileChunk);
    service.processChunk(fileChunk);
  }

  public String finishChunkedUpload(String sessionId) {
    FileChunkedUploadSession chunkedUploadSession = chunkedUploadSessions.get(sessionId);
    chunkedUploadSessions.remove(sessionId);

    return service.finishChunkedUpload(chunkedUploadSession);
  }

  public FileDownloadInfo getFileDownloadInfo(String fileId, String userId) throws FileDownloadValidateException {
    validateChunkedDownloadInfo(fileId, userId);
    return service.getFileDownloadInfo(fileId, userId);
  }

  public FileChunk getFileChunk(String currentFileId, int chunkIndex, int chunkSize) {
    return service.getFileChunk(currentFileId, chunkIndex, chunkSize);
  }

  private void validateChunkedDownloadInfo(String fileId, String userId) throws FileDownloadValidateException {
    if (fileId == null || fileId.trim().isEmpty())
      throw new FileDownloadValidateException("File ID is required");

    if (userId == null || userId.trim().isEmpty())
      throw new FileDownloadValidateException("User ID is required");
  }

  private void validateChunkedUploadDto(FileChunkedUploadSession chunkedUploadSession)
      throws FileUploadValidateException {
    if (chunkedUploadSession.sessionId() == null
        || chunkedUploadSession.sessionId().trim().isEmpty())
      throw new FileUploadValidateException("Session ID is required");

    if (chunkedUploadSession.ownerId() == null || chunkedUploadSession.ownerId().trim().isEmpty())
      throw new FileUploadValidateException("Owner ID is required");

    if (chunkedUploadSession.path() == null || chunkedUploadSession.path().trim().isEmpty())
      throw new FileUploadValidateException("File path is required");

    if (chunkedUploadSession.totalFileSize() < 0)
      throw new FileUploadValidateException("File size cannot be negative");

    if (chunkedUploadSession.totalChunks() <= 0)
      throw new FileUploadValidateException("Total chunks must be positive");

    if (chunkedUploadSession.totalFileSize() > StorageConfig.getInstance().getMaxFileSize())
      throw new FileUploadValidateException("File size exceeds maximum allowed limit");
  }

  private void validateChunkData(FileChunk fileChunk) throws FileUploadValidateException {
    if (fileChunk.sessionId() == null || fileChunk.sessionId().trim().isEmpty())
      throw new FileUploadValidateException("Session ID is required");

    if (fileChunk.chunkIndex() < 0)
      throw new FileUploadValidateException("Chunk index cannot be negative");

    if (fileChunk.chunkData() == null)
      throw new FileUploadValidateException("Chunk data cannot be null");

    if (fileChunk.chunkData().length == 0)
      throw new FileUploadValidateException("Chunk data cannot be empty");

    if (fileChunk.chunkData().length > StorageConfig.getInstance().getMaxFileChunkSize())
      throw new FileUploadValidateException("Chunk size exceeds maximum allowed limit");
  }
}

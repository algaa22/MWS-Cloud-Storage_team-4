package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunk;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadSession;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedDownloadInfo;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.GetFileInfoRequest;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;

import java.util.List;
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
      throws ValidationFailedException {
    chunkedUploadSession.validate();
    service.startChunkedUploadSession(chunkedUploadSession);
    chunkedUploadSessions.put(chunkedUploadSession.sessionId(), chunkedUploadSession);
  }

  public void processFileChunk(FileChunk fileChunk) throws ValidationFailedException {
    fileChunk.validate();
    service.processChunk(fileChunk);
  }

  public String finishChunkedUpload(String sessionId) {
    FileChunkedUploadSession chunkedUploadSession = chunkedUploadSessions.get(sessionId);
    chunkedUploadSessions.remove(sessionId);

    return service.finishChunkedUpload(chunkedUploadSession);
  }

  public FileChunkedDownloadInfo getFileDownloadInfo(GetFileInfoRequest request) throws ValidationFailedException {
    request.validate();
    return service.getFileDownloadInfo(request.fileId(), request.userId());
  }

  public FileChunk getFileChunk(String currentFileId, int chunkIndex, int chunkSize) {
    return service.getFileChunk(currentFileId, chunkIndex, chunkSize);
  }

  public List<String> getFilePathsList(String userId) {
    return service.getFilePathsList(userId);
  }
}

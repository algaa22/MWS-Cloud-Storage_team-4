package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.*;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import java.util.List;
import java.util.UUID;

public class FileController {
  private final FileService service;

  public FileController(FileService service) {
    this.service = service;
  }

  public void startChunkedUpload(FileChunkedUploadDto chunkedUploadRequest)
      throws ValidationFailedException {
    chunkedUploadRequest.validate();
    service.startChunkedUploadSession(chunkedUploadRequest);
  }

  public void processFileChunk(FileChunkDto fileChunk) throws ValidationFailedException {
    fileChunk.validate();
    service.processChunk(fileChunk);
  }

  public UUID finishChunkedUpload(UUID sessionId) {
    FileChunkedUploadDto chunkedUploadSession = chunkedUploadSessions.get(sessionId);
    chunkedUploadSessions.remove(sessionId);

    return service.finishChunkedUpload(sessionId);
  }

  public FileChunkedDownloadDto getFileDownloadInfo(GetFileInfoDto request)
      throws ValidationFailedException {
    request.validate();
    return service.getFileDownloadInfo(request.fileId(), request.userId());
  }

  public FileChunkDto getFileChunk(UUID currentFileId, int chunkIndex, int chunkSize) {
    return service.getFileChunk(currentFileId, chunkIndex, chunkSize);
  }

  public List<String> getFilePathsList(String userId) {
    return service.getFilePathsList(userId);
  }

  public FileDto getFileInfo(String fileId, String userId) {
    return service.getFileInfo(fileId, userId);
  }
}

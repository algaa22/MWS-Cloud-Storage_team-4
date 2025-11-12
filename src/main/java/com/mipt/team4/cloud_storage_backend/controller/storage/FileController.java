package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.exception.service.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.service.TranferSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.*;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;
import java.util.UUID;

public class FileController {
  private final FileService service;

  public FileController(FileService service) {
    this.service = service;
  }

  public void startChunkedUpload(FileChunkedUploadDto chunkedUploadRequest)
      throws ValidationFailedException, StorageFileAlreadyExistsException {
    chunkedUploadRequest.validate();
    service.startChunkedUploadSession(chunkedUploadRequest);
  }

  public void processFileChunk(FileChunkDto fileChunk)
      throws ValidationFailedException, TranferSessionNotFoundException {
    fileChunk.validate();
    service.processChunk(fileChunk);
  }

  public UUID finishChunkedUpload(String sessionId)
      throws MissingFilePartException, TranferSessionNotFoundException, ValidationFailedException {
    Validators.throwExceptionIfNotValid(Validators.isUuid("Session ID", sessionId));

    return service.finishChunkedUpload(sessionId);
  }

  public FileChunkedDownloadDto getFileDownloadInfo(GetFileInfoDto fileInfo)
      throws ValidationFailedException {
    fileInfo.validate();
    return service.getFileDownloadInfo(fileInfo.fileId(), fileInfo.userId());
  }

  public FileChunkDto getFileChunk(GetFileChunkDto fileChunk) throws ValidationFailedException {
    fileChunk.validate();
    return service.getFileChunk(fileChunk.fileId(), fileChunk.chunkIndex(), fileChunk.chunkSize());
  }

  public List<String> getFilePathsList(GetFilePathsListDto filePathsList)
      throws ValidationFailedException {
    filePathsList.validate();
    return service.getFilePathsList(filePathsList.userId());
  }

  public FileDto getFileInfo(GetFileInfoDto fileInfo) throws ValidationFailedException {
    fileInfo.validate();
    return service.getFileInfo(fileInfo.fileId(), fileInfo.userId());
  }
}

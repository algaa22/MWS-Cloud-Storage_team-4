package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
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
      throws ValidationFailedException, StorageFileAlreadyExistsException, StorageIllegalAccessException {
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

  public FileChunkedDownloadDto getFileDownloadInfo(SimpleFileOperationDto fileInfo)
      throws ValidationFailedException {
    fileInfo.validate();
    // TODO: Передавать DTO
    return service.getFileDownloadInfo(fileInfo.filePath(), fileInfo.userId());
  }

  public FileChunkDto getFileChunk(GetFileChunkDto fileChunkRequest)
      throws ValidationFailedException {
    fileChunkRequest.validate();
    // TODO: Передавать DTO
    return service.getFileChunk(
        fileChunkRequest.fileId(), fileChunkRequest.chunkIndex(), fileChunkRequest.chunkSize());
  }

  public List<String> getFilePathsList(GetFilePathsListDto filePathsListRequest)
      throws ValidationFailedException {
    filePathsListRequest.validate();
    // TODO: Передавать DTO
    return service.getFilePathsList(filePathsListRequest.userId());
  }

  public FileDto getFileInfo(SimpleFileOperationDto fileInfoRequest)
      throws ValidationFailedException {
    fileInfoRequest.validate();
    // TODO: Передавать DTO
    return service.getFileInfo(fileInfoRequest.filePath(), fileInfoRequest.userId());
  }

  public void deleteFile(SimpleFileOperationDto deleteFileRequest)
      throws ValidationFailedException, StorageIllegalAccessException {
    deleteFileRequest.validate();
    service.deleteFile(deleteFileRequest.userId(), deleteFileRequest.filePath());// TODO: в дто
  }
}

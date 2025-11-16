package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.*;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.io.FileNotFoundException;
import java.util.List;

public class FileController {
  private final FileService service;

  public FileController(FileService service) {
    this.service = service;
  }

  public void startChunkedUpload(FileChunkedUploadDto chunkedUploadRequest)
      throws ValidationFailedException,
          StorageFileAlreadyExistsException,
          StorageIllegalAccessException,
          UserNotFoundException {
    chunkedUploadRequest.validate();
    service.startChunkedUploadSession(chunkedUploadRequest);
  }

  public void processFileChunk(FileChunkDto fileChunk) throws ValidationFailedException {
    fileChunk.validate();
    service.processChunk(fileChunk);
  }

  public void completeChunkedUpload(String sessionId)
      throws MissingFilePartException,
          ValidationFailedException,
          StorageFileAlreadyExistsException,
          UserNotFoundException {
    Validators.throwExceptionIfNotValid(Validators.isUuid("Session ID", sessionId));

    service.completeChunkedUpload(sessionId);
  }

  public FileChunkedDownloadDto getFileDownloadInfo(SimpleFileOperationDto fileInfo)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageFileNotFoundException,
          StorageIllegalAccessException {
    fileInfo.validate();
    // TODO: Передавать DTO
    return service.getFileDownloadInfo(fileInfo.filePath(), fileInfo.userToken());
  }

  public FileChunkDto getFileChunk(GetFileChunkDto fileChunkRequest)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageFileNotFoundException,
          StorageIllegalAccessException {
    fileChunkRequest.validate();
    // TODO: Передавать DTO
    return service.getFileChunk(
        fileChunkRequest.fileId(), fileChunkRequest.chunkIndex(), fileChunkRequest.chunkSize());
  }

  public List<String> getFilePathsList(GetFilePathsListDto filePathsListRequest)
      throws ValidationFailedException, UserNotFoundException {
    filePathsListRequest.validate();
    // TODO: Передавать DTO
    return service.getFilePathsList(filePathsListRequest.userToken());
  }

  public FileDto getFileInfo(SimpleFileOperationDto fileInfoRequest)
      throws ValidationFailedException, UserNotFoundException, StorageFileNotFoundException {
    fileInfoRequest.validate();
    // TODO: Передавать DTO
    return service.getFileInfo(fileInfoRequest.userToken(), fileInfoRequest.filePath());
  }

  public void deleteFile(SimpleFileOperationDto deleteFileRequest)
      throws ValidationFailedException,
          StorageIllegalAccessException,
          UserNotFoundException,
          StorageFileNotFoundException,
          FileNotFoundException {
    deleteFileRequest.validate();
    service.deleteFile(deleteFileRequest.userToken(), deleteFileRequest.filePath()); // TODO: в дто
  }

  public void uploadFile(FileUploadDto fileUploadRequest)
      throws StorageFileAlreadyExistsException, ValidationFailedException, UserNotFoundException {
    fileUploadRequest.validate();
    service.uploadFile(fileUploadRequest);
  }
}

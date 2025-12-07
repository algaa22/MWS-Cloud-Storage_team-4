package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.CombineChunksToPartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
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

  public void startChunkedUpload(FileChunkedUploadDto request)
      throws ValidationFailedException,
          StorageFileAlreadyExistsException,
          StorageIllegalAccessException,
          UserNotFoundException {
    request.validate();
    service.startChunkedUploadSession(request);
  }

  public void processFileChunk(UploadChunkDto request)
      throws ValidationFailedException,
          UserNotFoundException,
          CombineChunksToPartException,
          UploadSessionNotFoundException {
    request.validate();
    service.uploadChunk(request);
  }

  public ChunkedUploadFileResultDto completeChunkedUpload(String request)
      throws MissingFilePartException,
          ValidationFailedException,
          StorageFileAlreadyExistsException,
          UserNotFoundException,
          TooSmallFilePartException,
          CombineChunksToPartException, UploadSessionNotFoundException {
    Validators.throwExceptionIfNotValid(Validators.isUuid("Session ID", request));

    return service.completeChunkedUpload(request);
  }

  public FileChunkedDownloadDto getFileDownloadInfo(SimpleFileOperationDto request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageEntityNotFoundException,
          StorageIllegalAccessException {
    request.validate();
    return service.getFileDownloadInfo(request);
  }

  public DownloadedChunkDto getFileChunk(GetFileChunkDto request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageEntityNotFoundException,
          StorageIllegalAccessException {
    request.validate();
    return service.getFileChunk(request);
  }

  public List<String> getFilePathsList(GetFilePathsListDto request)
      throws ValidationFailedException, UserNotFoundException {
    request.validate();
    return service.getFilePathsList(request);
  }

  public StorageDto getFileInfo(SimpleFileOperationDto request)
      throws ValidationFailedException, UserNotFoundException, StorageEntityNotFoundException {
    request.validate();
    return service.getFileInfo(request);
  }

  public void deleteFile(SimpleFileOperationDto request)
      throws ValidationFailedException,
          StorageIllegalAccessException,
          UserNotFoundException,
          StorageEntityNotFoundException,
          FileNotFoundException {
    request.validate();
    service.deleteFile(request);
  }

  public void uploadFile(FileUploadDto request)
      throws StorageFileAlreadyExistsException, ValidationFailedException, UserNotFoundException {
    request.validate();
    service.uploadFile(request);
  }

  public void changeFileMetadata(ChangeFileMetadataDto request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageEntityNotFoundException,
          StorageFileAlreadyExistsException {
    request.validate();
    service.changeFileMetadata(request);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationDto request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageEntityNotFoundException {
    request.validate();
    return service.downloadFile(request);
  }
}

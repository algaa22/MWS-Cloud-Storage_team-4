package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.CombineChunksToPartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChangeFileMetadataDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResultDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileUploadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.GetFileListDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.SimpleFileOperationDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.UploadChunkDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.io.FileNotFoundException;
import java.util.List;
import org.springframework.stereotype.Controller;

@Controller
public class FileController {
  // TODO: постоянный validate()
  private final FileService service;
  private final StorageConfig storageConfig;

  public FileController(FileService service, StorageConfig storageConfig) {
    this.service = service;
    this.storageConfig = storageConfig;
  }

  public void startChunkedUpload(FileChunkedUploadDto request)
      throws ValidationFailedException,
      StorageFileAlreadyExistsException,
      UserNotFoundException {
    request.validate();
    service.startChunkedUploadSession(request);
  }

  public void processFileChunk(UploadChunkDto request)
      throws ValidationFailedException,
      CombineChunksToPartException,
      UploadSessionNotFoundException {
    request.validate(storageConfig.rest().maxFileChunkSize());
    service.uploadChunk(request);
  }

  public ChunkedUploadFileResultDto completeChunkedUpload(String sessionId)
      throws MissingFilePartException,
      ValidationFailedException,
      StorageFileAlreadyExistsException,
      UserNotFoundException,
      TooSmallFilePartException,
      CombineChunksToPartException, UploadSessionNotFoundException {
    Validators.throwExceptionIfNotValid(Validators.isUuid("Session ID", sessionId));

    return service.completeChunkedUpload(sessionId);
  }

  public List<StorageEntity> getFileList(GetFileListDto request)
      throws ValidationFailedException, UserNotFoundException {
    request.validate();
    return service.getFileList(request);
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

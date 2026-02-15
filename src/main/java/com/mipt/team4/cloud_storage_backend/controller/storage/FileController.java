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
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResultDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDownloadDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleFileOperationRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadChunkRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.io.FileNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class FileController {
  // TODO: постоянный validate()
  private final FileService service;
  private final JwtService jwtService;
  private final StorageConfig storageConfig;

  public void startChunkedUpload(FileChunkedUploadRequest request)
      throws ValidationFailedException, StorageFileAlreadyExistsException, UserNotFoundException {
    request.validate(jwtService);
    service.startChunkedUploadSession(request);
  }

  public void processFileChunk(UploadChunkRequest request)
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
          CombineChunksToPartException,
          UploadSessionNotFoundException {
    Validators.throwExceptionIfNotValid(Validators.isUuid("Session ID", sessionId));

    return service.completeChunkedUpload(sessionId);
  }

  public List<StorageEntity> getFileList(GetFileListRequest request)
      throws ValidationFailedException, UserNotFoundException {
    request.validate(jwtService);
    return service.getFileList(request);
  }

  public StorageDto getFileInfo(SimpleFileOperationRequest request)
      throws ValidationFailedException, UserNotFoundException, StorageEntityNotFoundException {
    request.validate(jwtService);
    return service.getFileInfo(request);
  }

  public void deleteFile(SimpleFileOperationRequest request)
      throws ValidationFailedException,
          StorageIllegalAccessException,
          UserNotFoundException,
          StorageEntityNotFoundException,
          FileNotFoundException {
    request.validate(jwtService);
    service.deleteFile(request);
  }

  public void uploadFile(FileUploadRequest request)
      throws StorageFileAlreadyExistsException, ValidationFailedException, UserNotFoundException {
    request.validate(jwtService);
    service.uploadFile(request);
  }

  public void changeFileMetadata(ChangeFileMetadataRequest request)
      throws ValidationFailedException,
          UserNotFoundException,
          StorageEntityNotFoundException,
          StorageFileAlreadyExistsException {
    request.validate(jwtService);
    service.changeFileMetadata(request);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationRequest request)
      throws ValidationFailedException, UserNotFoundException, StorageEntityNotFoundException {
    request.validate(jwtService);
    return service.downloadFile(request);
  }
}

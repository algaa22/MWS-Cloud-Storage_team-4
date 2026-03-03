package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
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

  public void startChunkedUpload(FileChunkedUploadRequest request) {
    request.validate(jwtService);
    service.startChunkedUploadSession(request);
  }

  public void processFileChunk(UploadChunkRequest request) {
    request.validate(storageConfig.rest().maxFileChunkSize());
    service.uploadChunk(request);
  }

  public ChunkedUploadFileResultDto completeChunkedUpload(String sessionId) {
    Validators.throwExceptionIfNotValid(Validators.isUuid("Session ID", sessionId));

    return service.completeChunkedUpload(sessionId);
  }

  public List<StorageEntity> getFileList(GetFileListRequest request) {
    request.validate(jwtService);
    return service.getFileList(request);
  }

  public StorageDto getFileInfo(SimpleFileOperationRequest request) {
    request.validate(jwtService);
    return service.getFileInfo(request);
  }

  public void deleteFile(SimpleFileOperationRequest request) {
    request.validate(jwtService);
    service.deleteFile(request);
  }

  public void uploadFile(FileUploadRequest request) {
    request.validate(jwtService);
    return service.uploadFile(request);
  }

  public void changeFileMetadata(ChangeFileMetadataRequest request) {
    request.validate(jwtService);
    service.changeFileMetadata(request);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationRequest request) {
    request.validate(jwtService);
    return service.downloadFile(request);
  }
}

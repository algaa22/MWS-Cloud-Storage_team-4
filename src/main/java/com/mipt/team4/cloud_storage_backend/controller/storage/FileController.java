package com.mipt.team4.cloud_storage_backend.controller.storage;

import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResult;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ResumeChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleFileOperationRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SoftDeleteFileRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadChunkRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FileDownloadResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.service.storage.FileService;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class FileController {
  // TODO: постоянный validate()
  private final FileService service;
  private final AccessTokenService accessTokenService;
  private final StorageConfig storageConfig;

  public void startChunkedUpload(StartChunkedUploadRequest request) {
    request.validate(accessTokenService);
    service.startChunkedUpload(request);
  }

  public void processFileChunk(UploadChunkRequest request) {
    request.validate(storageConfig.rest().maxFileChunkSize());
    service.uploadChunk(request);
  }

  public ChunkedUploadFileResult completeChunkedUpload(String sessionId) {
    Validators.throwExceptionIfNotValid(Validators.isUuid("Session ID", sessionId)); // TODO: в DTO

    return service.completeChunkedUpload(sessionId);
  }

  public void resumeChunkedUpload(ResumeChunkedUploadRequest request) {
    request.validate();
    service.resumeChunkedUploadSession(request);
  }

  public List<StorageEntity> getFileList(GetFileListRequest request) {
    request.validate(accessTokenService);
    return service.getFileList(request);
  }

  public StorageDto getFileInfo(SimpleFileOperationRequest request) {
    request.validate(accessTokenService);
    return service.getFileInfo(request);
  }

  public void hardDeleteFile(SimpleFileOperationRequest request) {
    request.validate(accessTokenService);
    service.hardDeleteFile(request);
  }

  public void softDeleteFile(SoftDeleteFileRequest request) {
    request.validate(accessTokenService);
    service.softDeleteFile(request);
  }

  public UUID uploadFile(FileUploadRequest request) {
    request.validate(accessTokenService);
    return service.uploadFile(request);
  }

  public void changeFileMetadata(ChangeFileMetadataRequest request) {
    request.validate(accessTokenService);
    service.changeFileMetadata(request);
  }

  public void restoreFile(SimpleFileOperationRequest request) {
    request.validate(accessTokenService);
    service.restoreFile(request);
  }

  public List<StorageEntity> getTrashFileList(GetFileListRequest request) {
    request.validate(accessTokenService);
    return service.getTrashFileList(request);
  }

  public FileDownloadResponse downloadFile(SimpleFileOperationRequest request) {
    request.validate(accessTokenService);
    return service.downloadFile(request);
  }
}

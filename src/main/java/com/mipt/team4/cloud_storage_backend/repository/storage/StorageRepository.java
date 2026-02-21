package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StorageRepository {
  private final FileMetadataRepository metadataRepository;
  private final FileContentRepository contentRepository;
  private final StorageRepositoryWrapper wrapper;

  public void addFile(StorageEntity entity, byte[] data) {
    wrapper.executeCreateOperation(
        entity,
        FileOperationType.UPLOAD,
        () -> {
          metadataRepository.addFile(entity);
          contentRepository.putObject(entity.getS3Key(), data);

          return null;
        });
  }

  public String startMultipartUpload(StorageEntity entity) {
    return wrapper.executeStartComplexOperation(
        entity,
        FileOperationType.UPLOAD,
        () -> {
          metadataRepository.addFile(entity);
          return contentRepository.startMultipartUpload(entity.getS3Key());
        });
  }

  public String uploadPart(StorageEntity entity, UploadPartRequest request) {
    return wrapper.executeInProgressOperation(
        entity,
        FileOperationType.UPLOAD,
        () ->
            contentRepository.uploadPart(
                request.uploadId(), entity.getS3Key(), request.partIndex(), request.bytes()));
  }

  public void completeMultipartUpload(
      StorageEntity entity, long fileSize, String uploadId, Map<Integer, String> eTags) {
    wrapper.executeFinalComplexOperation(
        entity,
        FileOperationType.UPLOAD,
        () -> {
          entity.setSize(fileSize);
          contentRepository.completeMultipartUpload(entity.getS3Key(), uploadId, eTags);
          return null;
        });
  }

  public void updateFile(StorageEntity entity) {
    wrapper.executeUpdateOperation(entity, FileOperationType.CHANGE_METADATA, () -> null);
  }

  public void deleteFile(StorageEntity entity) {
    wrapper.executeUpdateOperation(
        entity,
        FileOperationType.DELETE,
        () -> {
          metadataRepository.deleteFile(entity.getUserId(), entity.getParentId(), entity.getName());
          contentRepository.hardDeleteFile(entity.getS3Key());
          return null;
        });
  }

  public InputStream downloadFile(StorageEntity entity) {
    if (entity.getStatus() != FileStatus.READY) {
      throw new IllegalStateException(
          "FATAL: Attempt to download non-ready file: " + entity.getId());
    }

    return contentRepository.downloadObject(entity.getS3Key());
  }

  public Optional<StorageEntity> getFile(UUID userId, UUID parentId, String name) {
    return metadataRepository.getFile(userId, parentId, name);
  }

  public Optional<StorageEntity> getFileById(UUID fileId) {
    return metadataRepository.getFileById(fileId);
  }

  public boolean fileExists(UUID userId, UUID parentId, String name) {
    return metadataRepository.fileExists(userId, parentId, name);
  }

  public List<StorageEntity> getFilesList(FileListFilter filter) {
    return metadataRepository.getFilesList(filter);
  }

  public void addDirectory(StorageEntity entity) throws StorageFileAlreadyExistsException {
    metadataRepository.addFile(entity);
  }
}

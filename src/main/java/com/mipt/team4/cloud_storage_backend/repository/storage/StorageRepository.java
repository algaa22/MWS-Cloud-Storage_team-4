package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
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
    wrapper.executeUpdateOperation(
        entity,
        FileOperationType.UPLOAD,
        (s3Key) -> {
          metadataRepository.addFile(entity);
          contentRepository.putObject(s3Key, data);

          return null;
        });
  }

  public String startMultipartUpload(StorageEntity entity) {
    return wrapper.executeUpdateOperation(
        entity, FileOperationType.UPLOAD, contentRepository::startMultipartUpload);
  }

  public String uploadPart(UploadPartRequest request) {
    return wrapper.executeUpdateOperation(
        request.fileId(),
        FileOperationType.UPLOAD,
        (s3Key) ->
            contentRepository.uploadPart(
                request.uploadId(), s3Key, request.partIndex(), request.bytes()));
  }

  public void completeMultipartUpload(
      StorageEntity entity, String uploadId, Map<Integer, String> eTags) {
    wrapper.executeUpdateOperation(
        entity,
        FileOperationType.UPLOAD,
        (s3Key) -> {
          metadataRepository.addFile(entity);
          contentRepository.completeMultipartUpload(s3Key, uploadId, eTags);
          return null;
        });
  }

  public void updateFile(StorageEntity entity) {
    wrapper.executeUpdateOperation(
        entity,
        FileOperationType.CHANGE_METADATA,
        (_) -> {
          metadataRepository.updateEntity(entity);
          return null;
        });
  }

  public void deleteFile(StorageEntity entity) {
    wrapper.executeUpdateOperation(
        entity,
        FileOperationType.DELETE,
        (s3Key) -> {
          metadataRepository.deleteFile(entity.getUserId(), entity.getPath());
          contentRepository.hardDeleteFile(s3Key);
          return null;
        });
  }

  public InputStream downloadFile(StorageEntity entity) {
    return contentRepository.downloadObject(entity.getS3Key());
  }

  public Optional<StorageEntity> getFile(UUID userId, String path) {
    return metadataRepository.getFile(userId, path);
  }

  public boolean fileExists(UUID userId, String path) {
    return metadataRepository.fileExists(userId, path);
  }

  public List<StorageEntity> getFileList(FileListFilter filter) {
    return metadataRepository.getFilesList(filter);
  }

  public void addDirectory(StorageEntity entity) throws StorageFileAlreadyExistsException {
    metadataRepository.addFile(entity);
  }
}

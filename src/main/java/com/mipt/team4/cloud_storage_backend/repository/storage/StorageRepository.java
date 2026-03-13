package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
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
    wrapper.wrapNewFileTask(
        entity,
        FileOperationType.UPLOAD,
        () -> {
          metadataRepository.addFile(entity);
          contentRepository.putObject(entity.getS3Key(), data);

          return null;
        });
  }

  public String startMultipartUpload(StorageEntity entity) {
    return wrapper.initiateStep(
        entity,
        FileOperationType.UPLOAD,
        () -> {
          metadataRepository.addFile(entity);
          return contentRepository.startMultipartUpload(entity.getS3Key());
        });
  }

  public String uploadPart(StorageEntity entity, UploadPartRequest request) {
    return wrapper.processStep(
        entity,
        FileOperationType.UPLOAD,
        () ->
            contentRepository.uploadPart(
                request.uploadId(), entity.getS3Key(), request.partIndex(), request.bytes()));
  }

  public void completeMultipartUpload(
      StorageEntity entity, long fileSize, String uploadId, Map<Integer, String> eTags) {
    wrapper.completeStep(
        entity,
        FileOperationType.UPLOAD,
        () -> {
          entity.setSize(fileSize);
          contentRepository.completeMultipartUpload(entity.getS3Key(), uploadId, eTags);
          return null;
        });
  }

  public void updateFile(StorageEntity entity) {
    wrapper.wrapUpdate(entity, FileOperationType.CHANGE_METADATA, () -> null);
  }

  public void deleteFile(StorageEntity entity) {
    wrapper.wrapUpdate(
        entity,
        FileOperationType.DELETE,
        () -> {
          metadataRepository.deleteFile(entity);
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

  public Optional<StorageEntity> getFile(UUID userId, UUID fileId) {
    return metadataRepository.getFile(userId, fileId);
  }

  public boolean fileExists(UUID userId, UUID parentId, String name) {
    return metadataRepository.fileExists(userId, parentId, name);
  }

  public List<StorageEntity> getFileList(FileListFilter filter) {
    return metadataRepository.getFileList(filter);
  }

  public void addDirectory(StorageEntity entity) {
    metadataRepository.addFile(entity);
  }

  public List<StorageEntity> getFilesByTags(UUID userId, List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return List.of();
    }
    return metadataRepository.getFilesByTags(userId, tags);
  }

  public String getFullFilePath(UUID fileId) {
    return metadataRepository.getFullFilePath(fileId);
  }
}

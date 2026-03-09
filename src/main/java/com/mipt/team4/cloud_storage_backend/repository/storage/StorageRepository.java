package com.mipt.team4.cloud_storage_backend.repository.storage;

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
  private final StorageJpaRepositoryAdapter metadataRepository;
  private final FileContentRepository contentRepository;
  private final StorageRepositoryWrapper wrapper;

  public void add(StorageEntity entity, byte[] data) {
    wrapper.wrapNewEntityTask(
        entity,
        FileOperationType.CREATE,
        () -> {
          metadataRepository.addFile(entity);
          contentRepository.putObject(entity.getS3Key(), data);

          return null;
        });
  }

  public String startMultipartUpload(StorageEntity entity) {
    return wrapper.initiateStep(
        entity,
        FileOperationType.CREATE,
        () -> {
          metadataRepository.addFile(entity);
          return contentRepository.startMultipartUpload(entity.getS3Key());
        });
  }

  public String uploadPart(StorageEntity entity, UploadPartRequest request) {
    return wrapper.processStep(
        entity,
        FileOperationType.CREATE,
        () ->
            contentRepository.uploadPart(
                request.uploadId(), entity.getS3Key(), request.partIndex(), request.bytes()));
  }

  public void completeMultipartUpload(
      StorageEntity entity, long fileSize, String uploadId, Map<Integer, String> eTags) {
    wrapper.completeStep(
        entity,
        FileOperationType.CREATE,
        () -> {
          entity.setSize(fileSize);
          contentRepository.completeMultipartUpload(entity.getS3Key(), uploadId, eTags);
          return null;
        });
  }

  public void hardDelete(StorageEntity entity) {
    wrapper.wrapUpdate(
        entity,
        FileOperationType.DELETE,
        () -> {
          if (entity.isDirectory()) {
            List<StorageEntity> descendants =
                metadataRepository.findAllDescendants(entity.getUserId(), entity.getId());

            for (StorageEntity file : descendants) {
              contentRepository.hardDelete(file.getS3Key());
            }
          }

          contentRepository.hardDelete(entity.getS3Key());
          metadataRepository.hardDelete(entity.getUserId(), entity.getId());

          return null;
        });
  }

  public void softDeleteEntity(StorageEntity entity) {
    wrapper.wrapUpdate(
        entity,
        FileOperationType.CHANGE_METADATA,
        () -> {
          metadataRepository.softDelete(entity.getUserId(), entity.getId(), entity.isDirectory());
          return null;
        });
  }

  public void restore(StorageEntity entity) {
    wrapper.wrapUpdate(
        entity,
        FileOperationType.CHANGE_METADATA,
        () -> {
          metadataRepository.restore(entity.getUserId(), entity.getId(), entity.isDirectory());
          return null;
        });
  }

  public void addDirectory(StorageEntity entity) {
    wrapper.wrapNewEntityTask(
        entity,
        FileOperationType.CREATE,
        () -> {
          metadataRepository.addFile(entity);
          return null;
        });
  }

  public List<StorageEntity> getTrashFileList(UUID userId, UUID parentId) {
    return metadataRepository.getTrashFileList(userId, parentId);
  }

  public InputStream download(StorageEntity entity) {
    if (entity.getStatus() != FileStatus.READY) {
      throw new IllegalStateException( // TODO: illegal state?
          "FATAL: Attempt to download non-ready file: " + entity.getId());
    }

    return contentRepository.downloadObject(entity.getS3Key());
  }

  public Optional<StorageEntity> get(UUID userId, UUID fileId) {
    return metadataRepository.get(userId, fileId);
  }

  public Optional<StorageEntity> getIncludeDeleted(UUID userId, UUID fileId) {
    return metadataRepository.getIncludeDeleted(userId, fileId);
  }

  public Optional<StorageEntity> getDeletedById(UUID userId, UUID fileId) {
    return metadataRepository.getDeletedById(userId, fileId);
  }

  public boolean exists(UUID userId, UUID parentId, String name) {
    return metadataRepository.exists(userId, parentId, name);
  }

  public List<StorageEntity> getFileList(FileListFilter filter) {
    return metadataRepository.getFileList(filter);
  }
}

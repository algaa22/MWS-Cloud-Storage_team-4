package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.DownloadNonReadyFileException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadPartEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.ChunkedUploadSessionEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.ChunkedUploadStatus;
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
  private final ChunkedUploadJpaRepositoryAdapter uploadRepository;
  private final StorageJpaRepositoryAdapter metadataRepository;
  private final FileContentRepository contentRepository;
  private final StorageRepositoryWrapper wrapper;

  public void add(StorageEntity entity, byte[] data) {
    wrapper.wrapNewEntityTask(
        entity,
        FileOperationType.UPLOAD,
        () -> {
          metadataRepository.addFile(entity);
          contentRepository.putObject(entity.getS3Key(), data);

          return null;
        });
  }

  public void startMultipartUpload(StorageEntity fileEntity, ChunkedUploadSessionEntity session) {
    wrapper.initiateStep(
        fileEntity,
        FileOperationType.UPLOAD,
        () -> {
          String uploadId = contentRepository.startMultipartUpload(fileEntity.getS3Key());

          session.setUploadId(uploadId);
          uploadRepository.addSession(session);

          return null;
        });
  }

  public void uploadPart(
      StorageEntity fileEntity,
      UUID sessionId,
      String uploadId,
      InputStream inputStream,
      ChunkedUploadPartEntity part) {
    wrapper.processStep(
        fileEntity,
        FileOperationType.UPLOAD,
        () -> {
          String eTag =
              contentRepository.uploadPart(
                  uploadId,
                  fileEntity.getS3Key(),
                  part.getPartNumber(),
                  inputStream,
                  part.getSize());

          part.setETag(eTag);
          uploadRepository.upsertPart(part);
          uploadRepository.incrementCurrentSize(sessionId, part.getSize());

          return null;
        });
  }

  public void completeMultipartUpload(
      StorageEntity entity, UUID sessionId, String uploadId, Map<Integer, String> eTags) {
    wrapper.completeStep(
        entity,
        FileOperationType.UPLOAD,
        () -> {
          contentRepository.completeMultipartUpload(entity.getS3Key(), uploadId, eTags);
          uploadRepository.deleteSession(sessionId);
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
        FileOperationType.UPLOAD,
        () -> {
          metadataRepository.addFile(entity);
          return null;
        });
  }

  public void updateUploadSessionStatus(
      UUID sessionId, ChunkedUploadStatus oldStatus, ChunkedUploadStatus newStatus) {
    uploadRepository.updateSessionStatus(sessionId, oldStatus, newStatus);
  }

  public List<StorageEntity> getTrashFileList(UUID userId, UUID parentId) {
    return metadataRepository.getTrashFileList(userId, parentId);
  }

  public InputStream download(StorageEntity entity) {
    if (entity.getStatus() != FileStatus.READY) {
      throw new DownloadNonReadyFileException(entity.getId());
    }

    return contentRepository.downloadObject(entity.getS3Key());
  }

  public Optional<StorageEntity> get(UUID userId, UUID fileId) {
    return metadataRepository.get(userId, fileId);
  }

  public Optional<StorageEntity> getIncludeDeleted(UUID userId, UUID fileId) {
    return metadataRepository.getIncludeDeleted(userId, fileId);
  }

  public Optional<StorageEntity> getIncludeDeleted(UUID userId, UUID parentId, String name) {
    return metadataRepository.getIncludeDeleted(userId, parentId, name);
  }

  public Optional<StorageEntity> getDeleted(UUID userId, UUID fileId) {
    return metadataRepository.getDeleted(userId, fileId);
  }

  public boolean exists(UUID userId, UUID parentId, String name) {
    return metadataRepository.exists(userId, parentId, name);
  }

  public List<StorageEntity> getFileList(FileListFilter filter) {
    return metadataRepository.getFileList(filter);
  }

  public String getFullFilePath(UUID fileId) {
    return metadataRepository.getFullFilePath(fileId);
  }

  public boolean isPartAlreadyUploaded(UUID sessionId, int partNumber) {
    return uploadRepository.isPartAlreadyUploaded(sessionId, partNumber);
  }

  public Optional<ChunkedUploadSessionEntity> getUploadSession(UUID sessionId) {
    return uploadRepository.getSession(sessionId);
  }
}

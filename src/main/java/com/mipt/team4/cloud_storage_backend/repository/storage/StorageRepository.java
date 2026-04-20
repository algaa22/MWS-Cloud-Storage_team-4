package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.DownloadNonReadyFileException;
import com.mipt.team4.cloud_storage_backend.exception.upload.IncorrectUploadStatusException;
import com.mipt.team4.cloud_storage_backend.model.common.dto.PageQuery;
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
import org.springframework.data.domain.Page;
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

  public void startChunkedUpload(StorageEntity entity, ChunkedUploadSessionEntity session) {
    wrapper.initiateNewFileStep(
        entity,
        FileOperationType.UPLOAD,
        () -> {
          metadataRepository.addFile(entity);

          String uploadId = contentRepository.startMultipartUpload(entity.getS3Key());
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
                  uploadId, fileEntity.getS3Key(), part.getNumber(), inputStream, part.getSize());

          int updatedRows = touchUploadSessionStatus(sessionId, ChunkedUploadStatus.UPLOADING);
          if (updatedRows == 0) {
            throw new IncorrectUploadStatusException(ChunkedUploadStatus.UPLOADING);
          }

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
        FileStatus.SCANNING,
        () -> {
          contentRepository.completeMultipartUpload(entity.getS3Key(), uploadId, eTags);
          uploadRepository.deleteSession(sessionId);
          return null;
        });
  }

  public void abortMultipartUpload(StorageEntity entity, UUID sessionId, String uploadId) {
    wrapper.wrapUpdate(
        entity,
        FileOperationType.UPLOAD,
        () -> {
          contentRepository.abortMultipartUpload(entity.getS3Key(), uploadId);
          uploadRepository.deleteSession(sessionId);
          metadataRepository.hardDelete(entity.getUserId(), entity.getId());
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
              if (file.getS3Key() != null) {
                contentRepository.hardDelete(file.getS3Key());
              }
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

  public void tryUpdateUploadSessionStatus(
      ChunkedUploadSessionEntity session,
      ChunkedUploadStatus expectedOldStatus,
      ChunkedUploadStatus newStatus) {
    int updatedRows =
        uploadRepository.updateSessionStatus(session.getId(), expectedOldStatus, newStatus);

    if (updatedRows == 0) {
      throw new IncorrectUploadStatusException(expectedOldStatus);
    }

    session.setStatus(newStatus);
  }

  public int touchUploadSessionStatus(UUID sessionId, ChunkedUploadStatus expectedStatus) {
    return uploadRepository.touchSessionStatus(sessionId, expectedStatus);
  }

  public Page<StorageEntity> getTrashFileList(UUID userId, UUID parentId, PageQuery pageQuery) {
    return metadataRepository.getTrashFileList(userId, parentId, pageQuery);
  }

  public InputStream download(StorageEntity entity, String range) {
    if (entity.getStatus() != FileStatus.READY) {
      throw new DownloadNonReadyFileException(entity.getId());
    }

    return contentRepository.downloadObject(entity.getS3Key(), range);
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

  public Page<StorageEntity> getFileList(FileListFilter filter, PageQuery pageQuery) {
    return metadataRepository.getFileList(filter, pageQuery);
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

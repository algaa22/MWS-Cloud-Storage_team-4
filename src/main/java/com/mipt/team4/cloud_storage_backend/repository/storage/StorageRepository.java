package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
import java.io.FileNotFoundException;
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
  private final StorageMetadataRepository metadataRepository;
  private final FileContentRepository contentRepository;

  public void addFile(StorageEntity storageEntity, byte[] data)
      throws StorageFileAlreadyExistsException {
    String s3Key = StoragePaths.getS3Key(storageEntity.getUserId(), storageEntity.getEntityId());

    metadataRepository.addFile(storageEntity);
    contentRepository.putObject(s3Key, data);
  }

  public Optional<StorageEntity> getFile(UUID userId, String path) {
    return metadataRepository.getFile(userId, path);
  }

  public boolean fileExists(UUID userId, String path) {
    return metadataRepository.fileExists(userId, path);
  }

  public String startMultipartUpload(UUID userId, UUID fileId) {
    String s3Key = StoragePaths.getS3Key(userId, fileId);
    return contentRepository.startMultipartUpload(s3Key);
  }

  public String uploadPart(UploadPartRequest request) {
    String s3Key = StoragePaths.getS3Key(request.userId(), request.fileId());
    return contentRepository.uploadPart(
        request.uploadId(), s3Key, request.partIndex(), request.bytes());
  }

  public List<StorageEntity> getFileList(FileListFilter filter) {
    return metadataRepository.getFilesList(filter);
  }

  public void completeMultipartUpload(
      StorageEntity storageEntity, String uploadId, Map<Integer, String> eTags)
      throws StorageFileAlreadyExistsException {
    String s3Key = StoragePaths.getS3Key(storageEntity.getUserId(), storageEntity.getEntityId());

    metadataRepository.addFile(storageEntity);
    contentRepository.completeMultipartUpload(s3Key, uploadId, eTags);
  }

  public InputStream downloadFile(StorageEntity storageEntity) {
    String s3Key = StoragePaths.getS3Key(storageEntity.getUserId(), storageEntity.getEntityId());
    return contentRepository.downloadObject(s3Key);
  }

  public void deleteFile(UUID userId, String path)
      throws StorageEntityNotFoundException, FileNotFoundException {
    metadataRepository.deleteFile(userId, path);
    contentRepository.hardDeleteFile(path);
  }

  public void updateFile(StorageEntity entity) {
    metadataRepository.updateEntity(entity);
  }

  public void deleteFile(StorageEntity storageEntity)
      throws StorageEntityNotFoundException {
    String s3Key = StoragePaths.getS3Key(storageEntity.getUserId(), storageEntity.getEntityId());

    metadataRepository.deleteFile(storageEntity.getUserId(), storageEntity.getPath());
    contentRepository.hardDeleteFile(s3Key);
  }

  public void addDirectory(StorageEntity directoryEntity) throws StorageFileAlreadyExistsException {
    metadataRepository.addFile(directoryEntity);
  }
}

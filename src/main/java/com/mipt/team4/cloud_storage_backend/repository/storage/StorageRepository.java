package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StorageRepository {
  FileMetadataRepository metadataRepository;
  FileContentRepository contentRepository;

  public StorageRepository(PostgresConnection postgresConnection, String minioUrl) {
    metadataRepository = new PostgresFileMetadataRepository(postgresConnection);
    contentRepository = new MinioContentRepository(minioUrl);
  }

  public void addFile(StorageEntity storageEntity, byte[] data) throws StorageFileAlreadyExistsException {
    String s3Key = StoragePaths.getS3Key(storageEntity.getUserId(), storageEntity.getEntityId());

    metadataRepository.addFile(storageEntity); // TODO: если ошибка в putObject
    contentRepository.putObject(s3Key, data, storageEntity.getMimeType());
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

  public String uploadPart(
      String uploadId, UUID userId, UUID fileId, int partIndex, byte[] bytes) {
    String s3Key = StoragePaths.getS3Key(userId, fileId);
    // TODO: параметры в дто?
    return contentRepository.uploadPart(uploadId, s3Key, partIndex, bytes);
  }

  public List<String> getFilePathsList(UUID userId, boolean includeDirectories, String searchDirectory) {
    return metadataRepository.getFilesPathsList(userId, includeDirectories, searchDirectory);
  }

  public void completeMultipartUpload(
          StorageEntity storageEntity, String uploadId, Map<Integer, String> eTags)
      throws StorageFileAlreadyExistsException {
    String s3Key = StoragePaths.getS3Key(storageEntity.getUserId(), storageEntity.getEntityId());

    metadataRepository.addFile(storageEntity);
    contentRepository.completeMultipartUpload(s3Key, uploadId, eTags);
  }

  public byte[] downloadFile(StorageEntity storageEntity) {
    String s3Key = StoragePaths.getS3Key(storageEntity.getUserId(), storageEntity.getEntityId());
    return contentRepository.downloadFile(s3Key);
  }

  public void deleteFile(UUID userId, String path)
      throws StorageFileNotFoundException, FileNotFoundException {
    metadataRepository.deleteFile(userId, path);
    contentRepository.hardDeleteFile(path);
  }

  public void updateFile(StorageEntity entity) {
    metadataRepository.updateFile(entity);
  }

  public byte[] downloadFilePart(UUID userId, UUID fileId, long offset, long actualChunkSize) {
    String s3Key = StoragePaths.getS3Key(userId, fileId);
    return contentRepository.downloadFilePart(s3Key, offset, actualChunkSize);
  }

  public void deleteFile(StorageEntity storageEntity)
          throws StorageFileNotFoundException, FileNotFoundException {
    String s3Key = StoragePaths.getS3Key(storageEntity.getUserId(), storageEntity.getEntityId());

    metadataRepository.deleteFile(storageEntity.getUserId(), storageEntity.getPath());
    contentRepository.hardDeleteFile(s3Key);
  }

  public void addDirectory(StorageEntity directoryEntity) throws StorageFileAlreadyExistsException {
    metadataRepository.addFile(directoryEntity);
  }
}

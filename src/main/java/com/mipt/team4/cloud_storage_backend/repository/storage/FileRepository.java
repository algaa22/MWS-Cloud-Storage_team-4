package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FileRepository {
  FileMetadataRepository metadataRepository;
  FileContentRepository contentRepository;

  public FileRepository(PostgresConnection postgresConnection) {
    metadataRepository = new PostgresFileMetadataRepository(postgresConnection);
    contentRepository = new MinioContentRepository();
  }

  public void addFile(FileEntity fileEntity, byte[] data) throws StorageFileAlreadyExistsException {
    metadataRepository.addFile(fileEntity);
    contentRepository.putObject(fileEntity.getS3Key(), data, fileEntity.getMimeType());
  }

  public Optional<FileEntity> getFile(UUID ownerId, String path) {
    return metadataRepository.getFile(ownerId, path);
  }

  public boolean fileExists(UUID ownerId, String storagePath) {
    return metadataRepository.fileExists(ownerId, storagePath);
  }

  public CompletableFuture<String> startMultipartUpload(String s3Key) {
    return contentRepository.startMultipartUpload(s3Key);
  }

  public CompletableFuture<String> uploadPart(
      CompletableFuture<String> uploadId, String s3Key, int partIndex, byte[] bytes) {
    // TODO: параметры в дто?
    return contentRepository.uploadPart(uploadId, s3Key, partIndex, bytes);
  }

  public void completeMultipartUpload(
      FileEntity fileEntity,
      CompletableFuture<String> uploadId,
      Map<Integer, CompletableFuture<String>> eTags) throws StorageFileAlreadyExistsException {
    metadataRepository.addFile(fileEntity);
    contentRepository.completeMultipartUpload(fileEntity.getS3Key(), uploadId, eTags);
  }

  public InputStream downloadFile(String storagePath) {
    return contentRepository.downloadFile(storagePath);
  }
}

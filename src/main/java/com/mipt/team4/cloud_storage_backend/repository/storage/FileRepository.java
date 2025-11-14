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
  PostgresFileMetadataRepository postgresMetadataRepository;
  MinioContentRepository minioContentRepository;

  public FileRepository(PostgresConnection postgres) {
    postgresMetadataRepository = new PostgresFileMetadataRepository(postgres);
  }

  public void addFile(FileEntity fileEntity) throws StorageFileAlreadyExistsException {
    postgresMetadataRepository.addFile(fileEntity);
  }

  public Optional<FileEntity> getFile(UUID ownerId, String path) {
    return postgresMetadataRepository.getFile(ownerId, path);
  }

  public boolean fileExists(UUID ownerId, String storagePath) {
    return postgresMetadataRepository.fileExists(ownerId, storagePath);
  }

  public CompletableFuture<String> startMultipartUpload(String s3Key) {
    return minioContentRepository.startMultipartUpload(s3Key);
  }

  public CompletableFuture<String> uploadPart(CompletableFuture<String> uploadId, String s3Key, int partIndex, byte[] bytes) {
    return minioContentRepository.uploadPart(uploadId, s3Key, partIndex, bytes);
  }

  public void completeMultipartUpload(
          String s3Key,
          CompletableFuture<String> uploadId,
          Map<Integer, CompletableFuture<String>> eTags) {
       minioContentRepository.completeMultipartUpload(s3Key, uploadId, eTags);
  }

  public InputStream downloadFile(String storagePath) {
    return minioContentRepository.downloadFile(storagePath);
  }

  public void putObject(String s3Key, InputStream stream, String contentType) {}
}

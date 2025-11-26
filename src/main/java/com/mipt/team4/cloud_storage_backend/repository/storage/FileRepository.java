package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FileRepository {
  FileMetadataRepository metadataRepository;
  FileContentRepository contentRepository;

  public FileRepository(PostgresConnection postgresConnection, String minioUrl) {
    metadataRepository = new PostgresFileMetadataRepository(postgresConnection);
    contentRepository = new MinioContentRepository(minioUrl);
  }

  public void addFile(FileEntity fileEntity, byte[] data) throws StorageFileAlreadyExistsException {
    String s3Key = StoragePaths.getS3Key(fileEntity.getOwnerId(), fileEntity.getFileId());

    metadataRepository.addFile(fileEntity); // TODO: если ошибка в putObject
    contentRepository.putObject(s3Key, data, fileEntity.getMimeType());
  }

  public Optional<FileEntity> getFile(UUID ownerId, String path) {
    return metadataRepository.getFile(ownerId, path);
  }

  public boolean fileExists(UUID ownerId, String path) {
    return metadataRepository.fileExists(ownerId, path);
  }

  public CompletableFuture<String> startMultipartUpload(UUID ownerId, UUID fileId) {
    String s3Key = StoragePaths.getS3Key(ownerId, fileId);
    return contentRepository.startMultipartUpload(s3Key);
  }

  public CompletableFuture<String> uploadPart(
      CompletableFuture<String> uploadId, UUID ownerId, UUID fileId, int partIndex, byte[] bytes) {
    String s3Key = StoragePaths.getS3Key(ownerId, fileId);
    // TODO: параметры в дто?
    return contentRepository.uploadPart(uploadId, s3Key, partIndex, bytes);
  }

  public List<String> getFilePathsList(UUID userId) {
    return metadataRepository.getFilesPathsList(userId);
  }

  public void completeMultipartUpload(
      FileEntity fileEntity,
      CompletableFuture<String> uploadId,
      Map<Integer, CompletableFuture<String>> eTags)
      throws StorageFileAlreadyExistsException {
    String s3Key = StoragePaths.getS3Key(fileEntity.getOwnerId(), fileEntity.getFileId());

    metadataRepository.addFile(fileEntity);
    contentRepository.completeMultipartUpload(s3Key, uploadId, eTags);
  }

  public byte[] downloadFile(FileEntity fileEntity)
      throws FileNotFoundException {
    String s3Key = StoragePaths.getS3Key(fileEntity.getOwnerId(), fileEntity.getFileId());
    return contentRepository.downloadFile(s3Key);
  }

  public void deleteFile(FileEntity fileEntity)
      throws StorageFileNotFoundException, FileNotFoundException {
    String s3Key = StoragePaths.getS3Key(fileEntity.getOwnerId(), fileEntity.getFileId());

    metadataRepository.deleteFile(fileEntity.getOwnerId(), fileEntity.getPath());
    contentRepository.deleteFile(s3Key);
  }

  public byte[] downloadFilePart(FileEntity fileEntity) {
    return null;
  }

  public void updateFile(FileEntity entity) {
    metadataRepository.updateFile(entity);
    // TODO: если надо переместить офк
    contentRepository.moveFile(entity);
  }
}

package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FileRepository {
  FileMetadataRepository metadataRepository;
  FileContentRepository contentRepository;

  public FileRepository(PostgresConnection postgresConnection, String minioUrl) {
    metadataRepository = new PostgresFileMetadataRepository(postgresConnection);
    contentRepository = new MinioContentRepository(minioUrl);
  }

  public void addFile(FileEntity fileEntity, byte[] data) throws StorageFileAlreadyExistsException {
    metadataRepository.addFile(fileEntity); // TODO: если ошибка в putObject
    contentRepository.putObject(fileEntity.getS3Key(), data, fileEntity.getMimeType());
  }

  public Optional<FileEntity> getFile(UUID ownerId, String path) {
    return metadataRepository.getFile(ownerId, path);
  }

  public boolean fileExists(UUID ownerId, String s3Key) {
    return metadataRepository.fileExists(ownerId, s3Key);
  }

  public String startMultipartUpload(String s3Key) {
    return contentRepository.startMultipartUpload(s3Key);
  }

  public String uploadPart(
      String uploadId, String s3Key, int partIndex, byte[] bytes) {
    // TODO: параметры в дто?
    return contentRepository.uploadPart(uploadId, s3Key, partIndex, bytes);
  }

  public List<String> getFilePathsList(UUID userId) {
    return metadataRepository.getFilesPathsList(userId);
  }

  public void completeMultipartUpload(
      FileEntity fileEntity,
      String uploadId,
      Map<Integer, String> eTags)
      throws StorageFileAlreadyExistsException {
    metadataRepository.addFile(fileEntity);
    contentRepository.completeMultipartUpload(fileEntity.getS3Key(), uploadId, eTags);
  }

  public byte[] downloadFile(String s3Key)
      throws FileNotFoundException {
    return contentRepository.downloadFile(s3Key);
  }

  public void deleteFile(UUID ownerId, String s3Key)
      throws StorageFileNotFoundException, FileNotFoundException {
    metadataRepository.deleteFile(ownerId, s3Key);
    contentRepository.hardDeleteFile(s3Key);
  }

  public byte[] downloadFilePart(String s3Key) {
    return null;
  }

  public void updateFile(FileEntity entity, String oldS3Key) {
    metadataRepository.updateFile(entity);
    // TODO: если надо переместить офк
    contentRepository.moveFile(entity, oldS3Key);
  }

    public byte[] downloadFilePart(String s3Key, long offset, long actualChunkSize) {
      return null;
    }
}

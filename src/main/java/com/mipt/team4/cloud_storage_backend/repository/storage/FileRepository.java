package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FileRepository {
  PostgresFileMetadataRepository postgresMetadataRepository;

  public FileRepository(PostgresConnection postgres) {
    postgresMetadataRepository = new PostgresFileMetadataRepository(postgres);
  }

  public void addFile(FileEntity fileEntity) throws DbExecuteUpdateException {
    postgresMetadataRepository.addFile(fileEntity);
  }

  public Optional<FileEntity> getFile(UUID ownerId, String path) throws DbExecuteQueryException {
    return postgresMetadataRepository.getFile(ownerId, path);
  }

  public boolean fileExists(UUID ownerId, String storagePath) throws DbExecuteQueryException {
      return postgresMetadataRepository.fileExists(ownerId, storagePath);
  }

  public String startMultipartUpload(String s3Key) {
    // TODO: return upload ID
      return "";
  }

  public String uploadPart(String uploadId, int partIndex, byte[] bytes) {
    // TODO: return eTag
      return "";
  }

  public UUID finishMultipartUpload(String s3Key, String uploadId, List<String> eTags) {
    // TODO: return file ID
      return null;
  }
}

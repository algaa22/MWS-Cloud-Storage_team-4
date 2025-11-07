package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
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
}

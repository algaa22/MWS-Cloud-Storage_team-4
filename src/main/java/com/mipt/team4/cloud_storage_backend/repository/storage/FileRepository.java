package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class FileRepository {
  PostgresMetadataRepository postgresMetadataRepository;

  public FileRepository(PostgresConnection postgres) {
    postgresMetadataRepository = new PostgresMetadataRepository(postgres);
  }

  public void addFile(FileEntity fileEntity) throws SQLException {
    postgresMetadataRepository.addFile(fileEntity);
  }

  public Optional<FileEntity> getFile(UUID ownerId, String path) throws SQLException {
    return postgresMetadataRepository.getFile(ownerId, path);
  }
}

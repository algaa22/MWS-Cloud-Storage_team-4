package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;

import java.util.UUID;

public class FileRepository {
    PostgresMetadataRepository postgresMetadataRepository = new PostgresMetadataRepository();

    public void addFile(FileEntity fileEntity) {
      postgresMetadataRepository.addFile(fileEntity);
    }

    public FileEntity getFile(UUID ownerId, String path) {
      return postgresMetadataRepository.getFile(ownerId, path);
    }
}

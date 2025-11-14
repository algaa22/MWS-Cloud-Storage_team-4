package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository {
  void addFile(FileEntity fileEntity) throws StorageFileAlreadyExistsException;

  Optional<FileEntity> getFile(UUID ownerID, String path);

  boolean fileExists(UUID ownerId, String storagePath);

  void deleteFile(UUID ownerId, String storagePath) throws FileNotFoundException;
}

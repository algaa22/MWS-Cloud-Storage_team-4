package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageEntityNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository {
  void addFile(StorageEntity fileEntity) throws StorageFileAlreadyExistsException;

  List<StorageEntity> getFilesList(UUID id, boolean includeDirectories, boolean recursive, String searchDirectory);

  Optional<StorageEntity> getFile(UUID userId, String path);

  boolean fileExists(UUID userId, String path);

  void deleteFile(UUID userId, String path)
      throws StorageEntityNotFoundException;

  void updateFile(StorageEntity fileEntity);
}

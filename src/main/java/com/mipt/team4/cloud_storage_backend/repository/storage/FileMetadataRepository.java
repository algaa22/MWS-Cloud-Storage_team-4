package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository {
  void addFile(FileEntity fileEntity) throws StorageFileAlreadyExistsException;

  List<String> getFilesPathsList(UUID id);

  Optional<FileEntity> getFile(UUID ownerID, String path);

  boolean fileExists(UUID ownerId, String storagePath);

  void deleteFile(UUID ownerId, String storagePath) throws FileNotFoundException, StorageFileNotFoundException;
}

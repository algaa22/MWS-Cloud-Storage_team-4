package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public interface FileMetadataRepository {
  // TODO: Безопасное хранение пароля в конфиге
  void addFile(FileEntity fileEntity) throws SQLException;
  Optional<FileEntity> getFile(UUID ownerID, String path) throws SQLException;
}

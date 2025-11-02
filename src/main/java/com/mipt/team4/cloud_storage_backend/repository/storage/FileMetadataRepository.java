package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import java.util.UUID;

public interface FileMetadataRepository {
  // TODO: Безопасное хранение пароля в конфиге
  void addFile(FileEntity fileEntity);
  FileEntity getFile(UUID ownerID, String path);
}

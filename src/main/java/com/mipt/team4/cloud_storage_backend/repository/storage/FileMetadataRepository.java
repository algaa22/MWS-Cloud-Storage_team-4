package com.mipt.team4.cloud_storage_backend.repository.storage;

public interface FileMetadataRepository {
  // TODO: Безопасное хранение пароля в конфиге
  void addFile();
  void getFile();
}

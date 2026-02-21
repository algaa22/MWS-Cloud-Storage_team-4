package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class StorageFileNotFoundException extends RuntimeException {
  public StorageFileNotFoundException(UUID parentId, String name) {
    super("File or directory not found: parentId=" + parentId + "; name=" + name);
  }

  public StorageFileNotFoundException(UUID id) {
    super("File or directory not found: Id=" + id);
  }
}

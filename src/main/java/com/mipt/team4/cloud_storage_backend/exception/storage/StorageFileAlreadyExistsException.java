package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class StorageFileAlreadyExistsException extends RuntimeException {

  public StorageFileAlreadyExistsException(UUID parentId, String name) {
    super("File already exists: parent_id=" + parentId + "; name=" + name);
  }
}

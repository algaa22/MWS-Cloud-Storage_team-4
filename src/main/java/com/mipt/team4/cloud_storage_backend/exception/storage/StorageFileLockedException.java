package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class StorageFileLockedException extends RuntimeException {
  public StorageFileLockedException(UUID parentId, String name) {
    super("File locked by other operation: " + parentId + " " + name);
  }
}

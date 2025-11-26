package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class StorageFileAlreadyExistsException extends Exception {
  public StorageFileAlreadyExistsException(UUID ownerId, String filePath) {
    super("File with path " + filePath + " already exists for user " + ownerId);
  }
}

package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class StorageFileAlreadyExistsException extends Exception {
  public StorageFileAlreadyExistsException(UUID ownerId, String storagePath) {
    super("File with path " + storagePath + " already exists for user " + ownerId);
  }
}

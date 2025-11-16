package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class StorageFileNotFoundException extends Exception {
  public StorageFileNotFoundException(String storagePath) {
    super("File with path " + storagePath + " not found");
  }
}

package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class StorageFileAlreadyExistsException extends Exception {
  public StorageFileAlreadyExistsException(String filePath) {
    super("File already exists: path=" + filePath);
  }
}

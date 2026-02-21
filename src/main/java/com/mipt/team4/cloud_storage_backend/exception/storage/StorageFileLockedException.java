package com.mipt.team4.cloud_storage_backend.exception.storage;

public class StorageFileLockedException extends RuntimeException {
  public StorageFileLockedException(String path) {
    super("File locked by other operation: " + path);
  }
}

package com.mipt.team4.cloud_storage_backend.exception.storage;

public class StorageObjectNotFoundException extends StorageException {
  public StorageObjectNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}

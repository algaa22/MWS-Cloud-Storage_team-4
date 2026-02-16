package com.mipt.team4.cloud_storage_backend.exception.storage;

public class FatalStorageException extends StorageException {
  public FatalStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}

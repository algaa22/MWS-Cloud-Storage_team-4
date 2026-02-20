package com.mipt.team4.cloud_storage_backend.exception.storage;

public class RecoverableStorageException extends StorageException {
  public RecoverableStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}

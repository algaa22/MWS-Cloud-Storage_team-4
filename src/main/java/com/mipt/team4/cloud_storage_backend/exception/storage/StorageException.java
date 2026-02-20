package com.mipt.team4.cloud_storage_backend.exception.storage;

public abstract class StorageException extends RuntimeException {
  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }
}

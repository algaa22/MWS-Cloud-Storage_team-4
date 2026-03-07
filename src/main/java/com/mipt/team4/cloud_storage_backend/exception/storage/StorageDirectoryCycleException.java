package com.mipt.team4.cloud_storage_backend.exception.storage;

public class StorageDirectoryCycleException extends RuntimeException {
  public StorageDirectoryCycleException(String message) {
    super(message);
  }
}

package com.mipt.team4.cloud_storage_backend.exception.storage;

public class StorageDirectoryNotFoundException extends Exception {
  public StorageDirectoryNotFoundException(String path) {
    super("Directory not found: path=" + path);
  }
}

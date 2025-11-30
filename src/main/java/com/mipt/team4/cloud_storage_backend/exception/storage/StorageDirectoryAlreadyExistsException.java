package com.mipt.team4.cloud_storage_backend.exception.storage;

public class StorageDirectoryAlreadyExistsException extends Exception {
  public StorageDirectoryAlreadyExistsException(String directoryPath) {
    super("Directory already exists: path=" + directoryPath);
  }
}

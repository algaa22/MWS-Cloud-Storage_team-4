package com.mipt.team4.cloud_storage_backend.exception.storage;


public class StorageFileNotFoundException extends Exception {
  public StorageFileNotFoundException(String path) {
    super("File not found: path=" + path);
  }
}

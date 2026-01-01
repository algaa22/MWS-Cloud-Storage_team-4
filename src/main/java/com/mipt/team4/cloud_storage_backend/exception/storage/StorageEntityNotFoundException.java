package com.mipt.team4.cloud_storage_backend.exception.storage;


public class StorageEntityNotFoundException extends Exception {

  public StorageEntityNotFoundException(String path) {
    super("File or directory not found: path=" + path);
  }
}

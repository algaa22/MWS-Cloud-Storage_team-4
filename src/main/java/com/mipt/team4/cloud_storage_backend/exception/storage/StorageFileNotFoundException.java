package com.mipt.team4.cloud_storage_backend.exception.storage;


public class StorageFileNotFoundException extends Exception {
  public StorageFileNotFoundException(String storagePath) {
    super("File with path " + storagePath + " not found");
  }
}

package com.mipt.team4.cloud_storage_backend.exception.database;

public class StorageIllegalAccessException extends Exception {
  public StorageIllegalAccessException() {
    super("Access denied: user is not file owner.");
  }
}

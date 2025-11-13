package com.mipt.team4.cloud_storage_backend.exception.database;

public class StorageIllegalAccessException extends RuntimeException {
  public StorageIllegalAccessException(String message) {
    super("You don't have the rights to do this.");
  }
}

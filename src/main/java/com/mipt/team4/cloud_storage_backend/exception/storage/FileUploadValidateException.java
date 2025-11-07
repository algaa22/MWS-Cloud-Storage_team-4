package com.mipt.team4.cloud_storage_backend.exception.storage;

public class FileUploadValidateException extends Exception {
  public FileUploadValidateException(String message) {
    super("Validation exception at file chunked upload: " + message);
  }
}

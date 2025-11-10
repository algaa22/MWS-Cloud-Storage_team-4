package com.mipt.team4.cloud_storage_backend.exception.http.validation;

public class FileUploadValidationException extends Exception {
  public FileUploadValidationException(String message) {
    super("Validation exception at file chunked upload: " + message);
  }
}

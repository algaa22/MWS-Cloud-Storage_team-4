package com.mipt.team4.cloud_storage_backend.exception.http.validation;

public class FileDownloadValidationException extends Exception {
  public FileDownloadValidationException(String message) {
    super("Validation exception at file chunked download: " + message);
  }
}

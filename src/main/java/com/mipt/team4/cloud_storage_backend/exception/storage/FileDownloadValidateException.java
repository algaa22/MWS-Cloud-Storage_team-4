package com.mipt.team4.cloud_storage_backend.exception.storage;

public class FileDownloadValidateException extends Exception {
  public FileDownloadValidateException(String message) {
    super("Validation exception at file chunked download: " + message);
  }
}

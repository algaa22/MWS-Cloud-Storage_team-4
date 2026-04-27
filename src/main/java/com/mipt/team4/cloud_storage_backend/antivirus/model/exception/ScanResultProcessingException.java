package com.mipt.team4.cloud_storage_backend.antivirus.model.exception;

public class ScanResultProcessingException extends RuntimeException {
  public ScanResultProcessingException(String message) {
    super(message);
  }

  public ScanResultProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}

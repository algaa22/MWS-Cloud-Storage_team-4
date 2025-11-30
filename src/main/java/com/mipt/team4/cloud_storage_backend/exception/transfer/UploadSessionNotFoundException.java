package com.mipt.team4.cloud_storage_backend.exception.transfer;

public class UploadSessionNotFoundException extends Exception {
  public UploadSessionNotFoundException(String sessionId) {
    super("Upload session not found: sessionId=" + sessionId);
  }
}

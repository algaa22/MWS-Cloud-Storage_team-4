package com.mipt.team4.cloud_storage_backend.antivirus.model.exception;

import java.util.UUID;

public class ScannedFileOwnerNotFoundException extends ScanResultProcessingException {
  public ScannedFileOwnerNotFoundException(UUID fileId) {
    super("Owner of file with id " + fileId + " not found.");
  }
}

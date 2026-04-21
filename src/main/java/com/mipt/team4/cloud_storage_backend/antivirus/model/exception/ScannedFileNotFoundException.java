package com.mipt.team4.cloud_storage_backend.antivirus.model.exception;

import java.util.UUID;

public class ScannedFileNotFoundException extends ScanResultProcessingException {
  public ScannedFileNotFoundException(UUID fileId) {
    super(
        "File with id "
            + fileId
            + " not found. "
            + "Possible concurrent deletion or database desync.");
  }
}

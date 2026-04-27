package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class FileUnderScanException extends FileLockedException {
  public FileUnderScanException(UUID fileId) {
    super(
        "File %s is being scanned for threats. Access is temporarily restricted."
            .formatted(fileId));
  }
}

package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class FileIsDangerousException extends FileLockedException {
  public FileIsDangerousException(UUID fileId) {
    super(
        String.format(
            "Access denied: File [%s] is flagged as dangerous and has been locked for security reasons.",
            fileId));
  }
}

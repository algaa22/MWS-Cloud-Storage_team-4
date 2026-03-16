package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import java.util.UUID;

public class DownloadNonReadyFileException extends FatalStorageException {
  public DownloadNonReadyFileException(UUID fileId) {
    super("Attempt to download non-ready file: " + fileId);
  }
}

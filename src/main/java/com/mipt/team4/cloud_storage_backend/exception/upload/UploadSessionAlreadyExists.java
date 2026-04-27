package com.mipt.team4.cloud_storage_backend.exception.upload;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import java.util.UUID;

public class UploadSessionAlreadyExists extends FatalStorageException {

  public UploadSessionAlreadyExists(UUID sessionId) {
    super("Upload with sessionId " + sessionId + " already exists");
  }
}

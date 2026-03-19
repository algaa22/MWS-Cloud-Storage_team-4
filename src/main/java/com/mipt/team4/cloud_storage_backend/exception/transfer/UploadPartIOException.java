package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import java.io.IOException;

public class UploadPartIOException extends FatalStorageException {
  public UploadPartIOException(IOException cause) {
    super("Failed to upload part", cause);
  }
}

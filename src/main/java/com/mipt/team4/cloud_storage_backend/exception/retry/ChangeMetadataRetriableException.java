package com.mipt.team4.cloud_storage_backend.exception.retry;

import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;

public class ChangeMetadataRetriableException extends RetriableException {
  public ChangeMetadataRetriableException(RecoverableStorageException cause) {
    super("CHANGE METADATA", cause);
  }
}

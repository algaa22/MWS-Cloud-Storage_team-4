package com.mipt.team4.cloud_storage_backend.exception.retry;

import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;

public class CompleteUploadRetriableException extends UploadRetriableException {
  public CompleteUploadRetriableException(Throwable cause) {
    super("COMPLETE UPLOAD", cause);
  }
}

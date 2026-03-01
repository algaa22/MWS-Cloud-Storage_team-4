package com.mipt.team4.cloud_storage_backend.exception.retry;

import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import lombok.Getter;

public class UploadRetriableException extends RetriableException {
  public UploadRetriableException(String message, Throwable cause) {
    super(message, cause);
  }

  public UploadRetriableException(Throwable cause) {
    super("UPLOAD", cause);
  }
}

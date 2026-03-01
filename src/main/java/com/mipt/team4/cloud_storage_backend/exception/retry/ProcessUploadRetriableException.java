package com.mipt.team4.cloud_storage_backend.exception.retry;

import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import lombok.Getter;

@Getter
public class ProcessUploadRetriableException extends UploadRetriableException {
  private final long currentFileSize;
  private final int partNum;

  public ProcessUploadRetriableException(long currentFileSize, int partNum, Throwable cause) {
    super("PROCESS CHUNK", cause);

    this.currentFileSize = currentFileSize;
    this.partNum = partNum;
  }
}

package com.mipt.team4.cloud_storage_backend.exception.retry;

import lombok.Getter;

@Getter
public class ProcessUploadRetriableException extends UploadRetriableException {
  private final int partNum;

  public ProcessUploadRetriableException(int partNum, Throwable cause) {
    super("PROCESS CHUNK", cause);

    this.partNum = partNum;
  }
}

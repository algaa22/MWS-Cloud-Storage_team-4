package com.mipt.team4.cloud_storage_backend.exception.upload;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class UploadSizeMismatchException extends BaseStorageException {
  public UploadSizeMismatchException(long expected, long actual) {
    super(
        "Size mismatch during upload: expected=%s, actual=%s".formatted(expected, actual),
        HttpResponseStatus.CONFLICT);
  }
}

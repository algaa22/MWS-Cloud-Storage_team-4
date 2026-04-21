package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class FileLockedException extends BaseStorageException {
  public FileLockedException(String message) {
    super(message, HttpResponseStatus.CONFLICT);
  }
}

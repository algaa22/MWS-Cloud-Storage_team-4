package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class StorageFileLockedException extends BaseStorageException {
  public StorageFileLockedException(String path) {
    super("File locked by other operation: " + path, HttpResponseStatus.CONFLICT);
  }
}

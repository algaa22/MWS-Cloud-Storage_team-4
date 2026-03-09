package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.UUID;

public class StorageFileLockedException extends BaseStorageException {
  public StorageFileLockedException(UUID parentId, String name) {
    super(
        "File locked by other operation: parentId=" + parentId + ", name=" + name,
        HttpResponseStatus.CONFLICT);
  }
}

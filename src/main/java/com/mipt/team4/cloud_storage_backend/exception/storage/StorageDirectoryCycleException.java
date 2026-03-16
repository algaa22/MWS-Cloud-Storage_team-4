package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class StorageDirectoryCycleException extends BaseStorageException {
  public StorageDirectoryCycleException() {
    super("Cannot move directory into its own sub-directory", HttpResponseStatus.BAD_REQUEST);
  }
}

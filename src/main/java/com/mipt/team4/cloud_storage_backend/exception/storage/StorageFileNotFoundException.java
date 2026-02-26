package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class StorageFileNotFoundException extends BaseStorageException {
  public StorageFileNotFoundException(String path) {
    super("File or directory not found: path=" + path, HttpResponseStatus.NOT_FOUND);
  }
}

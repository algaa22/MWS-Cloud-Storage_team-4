package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class StorageFileAlreadyExistsException extends BaseStorageException {

  public StorageFileAlreadyExistsException(String filePath) {
    super("File already exists: path=" + filePath, HttpResponseStatus.CONFLICT);
  }
}

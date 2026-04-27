package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.UUID;

public class StorageFileNotFoundException extends BaseStorageException {

  public StorageFileNotFoundException(UUID fileId) {
    super("File not found with id: " + fileId, HttpResponseStatus.NOT_FOUND);
  }

  public StorageFileNotFoundException(String message) {
    super(message, HttpResponseStatus.NOT_FOUND);
  }
}

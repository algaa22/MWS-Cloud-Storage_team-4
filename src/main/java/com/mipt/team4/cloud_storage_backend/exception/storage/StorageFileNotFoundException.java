package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.UUID;

public class StorageFileNotFoundException extends BaseStorageException {
  public StorageFileNotFoundException(UUID parentId, String name) {
    super(
        "File or directory not found: id=" + parentId + "; name=" + name,
        HttpResponseStatus.NOT_FOUND);
  }

  public StorageFileNotFoundException(UUID id) {
    super("File or directory not found: id=" + id, HttpResponseStatus.NOT_FOUND);
  }
}

package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class StorageFileNotFoundException extends BaseStorageException {
  public StorageFileNotFoundException(UUID parentId, String name) {
    super(
        "File or directory not found: parentId=" + parentId + "; name=" + name,
        HttpResponseStatus.NOT_FOUND);
  }

  public StorageFileNotFoundException(UUID id) {
    super("File or directory not found: fileId=" + id, HttpResponseStatus.NOT_FOUND);
  }
}

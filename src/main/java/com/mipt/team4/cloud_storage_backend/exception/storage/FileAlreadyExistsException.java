package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.UUID;

public class FileAlreadyExistsException extends BaseStorageException {

  public FileAlreadyExistsException(UUID parentId, String name) {
    super(
        "File already exists: parent_id=" + parentId + "; name=" + name,
        HttpResponseStatus.CONFLICT);
  }
}

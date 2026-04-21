package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.UUID;

public class CannotDownloadDirectoryException extends BaseStorageException {

  public CannotDownloadDirectoryException(UUID id) {
    super("Cannot download directory with id " + id, HttpResponseStatus.BAD_REQUEST);
  }
}

package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class MoveDirectoryIntoItselfException extends BaseStorageException {
  public MoveDirectoryIntoItselfException() {
    super("Cannot move directory into itself", HttpResponseStatus.BAD_REQUEST);
  }
}

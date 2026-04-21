package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class S3ObjectNotFoundException extends BaseStorageException {
  public S3ObjectNotFoundException(String message, Throwable cause) {
    super(message, cause, HttpResponseStatus.NOT_FOUND);
  }
}

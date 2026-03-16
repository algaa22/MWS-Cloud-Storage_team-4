package com.mipt.team4.cloud_storage_backend.exception.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class NotFullRequestException extends FatalStorageException {
  public NotFullRequestException(String paramName) {
    super("Cannot read request body because request is not full: param=" + paramName);
  }
}

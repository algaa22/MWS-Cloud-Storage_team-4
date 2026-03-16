package com.mipt.team4.cloud_storage_backend.exception.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class WriteJsonBodyException extends FatalStorageException {
  public WriteJsonBodyException(Throwable cause) {
    super("Failed to write json body to response", cause);
  }
}

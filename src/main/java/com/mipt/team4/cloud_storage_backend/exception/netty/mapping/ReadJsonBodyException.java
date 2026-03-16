package com.mipt.team4.cloud_storage_backend.exception.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class ReadJsonBodyException extends FatalStorageException {
  public ReadJsonBodyException(Throwable cause) {
    super("Failed to read JSON body", cause);
  }
}

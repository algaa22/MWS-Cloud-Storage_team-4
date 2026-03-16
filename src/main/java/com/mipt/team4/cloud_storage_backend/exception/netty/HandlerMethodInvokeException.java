package com.mipt.team4.cloud_storage_backend.exception.netty;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class HandlerMethodInvokeException extends FatalStorageException {
  public HandlerMethodInvokeException(Throwable cause) {
    super("Failed to invoke handler method", cause);
  }
}

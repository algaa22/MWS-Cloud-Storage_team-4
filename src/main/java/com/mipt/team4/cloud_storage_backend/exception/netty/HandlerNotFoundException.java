package com.mipt.team4.cloud_storage_backend.exception.netty;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class HandlerNotFoundException extends FatalStorageException {
  public HandlerNotFoundException(Class<?> dtoClass) {
    super("Handler for class " + dtoClass.getSimpleName() + " was not found");
  }
}

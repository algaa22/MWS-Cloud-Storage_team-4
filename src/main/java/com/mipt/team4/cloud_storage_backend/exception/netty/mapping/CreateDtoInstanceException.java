package com.mipt.team4.cloud_storage_backend.exception.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class CreateDtoInstanceException extends FatalStorageException {

  public CreateDtoInstanceException(Class<?> clazz, Throwable cause) {
    super("Failed to create DTO instance for class " + clazz.getSimpleName());
  }
}

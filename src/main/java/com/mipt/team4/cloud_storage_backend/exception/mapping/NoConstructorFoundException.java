package com.mipt.team4.cloud_storage_backend.exception.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class NoConstructorFoundException extends FatalStorageException {
  public NoConstructorFoundException(Class<?> clazz) {
    super("No constructor found for class " + clazz.getSimpleName());
  }
}

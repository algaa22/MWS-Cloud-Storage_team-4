package com.mipt.team4.cloud_storage_backend.exception.utils;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class UnknownParamTypeException extends FatalStorageException {
  public UnknownParamTypeException(String name, Class<?> type) {
    super("Unknown param type: name=%s, type=%s".formatted(name, type.getSimpleName()));
  }
}

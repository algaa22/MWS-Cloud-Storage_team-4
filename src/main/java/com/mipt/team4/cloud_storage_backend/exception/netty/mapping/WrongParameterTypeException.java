package com.mipt.team4.cloud_storage_backend.exception.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class WrongParameterTypeException extends FatalStorageException {
  public WrongParameterTypeException(String paramName, Class<?> expectedType, Class<?> actualType) {
    super(
        "Wrong parameter type: param=%s, expected=%s, actual=%s"
            .formatted(paramName, expectedType.getSimpleName(), actualType.getSimpleName()));
  }
}

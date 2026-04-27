package com.mipt.team4.cloud_storage_backend.exception.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class ParseJsonParamException extends FatalStorageException {

  public ParseJsonParamException(String name, Class<?> type, Throwable cause) {
    super(
        "Failed to parse JSON param: name=%s, type=%s".formatted(name, type.getSimpleName()),
        cause);
  }
}

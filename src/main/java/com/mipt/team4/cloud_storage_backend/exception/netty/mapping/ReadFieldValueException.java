package com.mipt.team4.cloud_storage_backend.exception.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class ReadFieldValueException extends FatalStorageException {
  public ReadFieldValueException(String name, Throwable cause) {
    super("Failed to read field value: name=" + name, cause);
  }
}

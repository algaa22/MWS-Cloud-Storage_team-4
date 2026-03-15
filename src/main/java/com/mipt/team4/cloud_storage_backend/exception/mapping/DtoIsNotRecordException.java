package com.mipt.team4.cloud_storage_backend.exception.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class DtoIsNotRecordException extends FatalStorageException {
  public DtoIsNotRecordException(Class<?> clazz) {
    super("Class " + clazz.getSimpleName() + " is not a Record");
  }
}

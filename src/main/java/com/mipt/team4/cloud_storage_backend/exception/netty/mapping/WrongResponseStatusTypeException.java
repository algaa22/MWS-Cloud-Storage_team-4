package com.mipt.team4.cloud_storage_backend.exception.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class WrongResponseStatusTypeException extends FatalStorageException {
  public WrongResponseStatusTypeException(Class<?> clazz) {
    super(
        "Response status"
            + clazz.getSimpleName()
            + " has wrong type: class="
            + clazz.getSimpleName());
  }
}

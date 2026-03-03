package com.mipt.team4.cloud_storage_backend.exception.user;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class WrongPasswordException extends BaseStorageException {

  public WrongPasswordException() {
    super("Password incorrect", HttpResponseStatus.NOT_FOUND);
  }
}

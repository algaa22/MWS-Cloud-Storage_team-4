package com.mipt.team4.cloud_storage_backend.exception.user;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class UserNotFoundException extends BaseStorageException {

  public UserNotFoundException(String token) {
    super("User with token " + token + " not found", HttpResponseStatus.NOT_FOUND);
  }
}

package com.mipt.team4.cloud_storage_backend.exception.user.auth;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class MissingAuthTokenException extends BaseStorageException {
  public MissingAuthTokenException() {
    super("Missing auth token", HttpResponseStatus.UNAUTHORIZED);
  }
}

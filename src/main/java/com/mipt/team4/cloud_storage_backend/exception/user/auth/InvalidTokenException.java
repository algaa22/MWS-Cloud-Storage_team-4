package com.mipt.team4.cloud_storage_backend.exception.user.auth;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class InvalidTokenException extends BaseStorageException {
  public InvalidTokenException() {
    super("Invalid or expired token", HttpResponseStatus.UNAUTHORIZED);
  }
}

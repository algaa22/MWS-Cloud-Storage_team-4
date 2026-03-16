package com.mipt.team4.cloud_storage_backend.exception.user.auth;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class InvalidRefreshTokenException extends BaseStorageException {
  public InvalidRefreshTokenException() {
    super("Refresh token invalid or expired", HttpResponseStatus.UNAUTHORIZED);
  }
}

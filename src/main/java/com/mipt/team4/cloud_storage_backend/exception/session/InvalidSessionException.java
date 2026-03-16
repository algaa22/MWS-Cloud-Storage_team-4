package com.mipt.team4.cloud_storage_backend.exception.session;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class InvalidSessionException extends BaseStorageException {
  public InvalidSessionException() {
    super("No session with specified token was found", HttpResponseStatus.UNAUTHORIZED);
  }
}

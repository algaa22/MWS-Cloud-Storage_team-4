package com.mipt.team4.cloud_storage_backend.exception.share;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class InvalidSharePasswordException extends BaseStorageException {
  public InvalidSharePasswordException() {
    super("Invalid password for share link", HttpResponseStatus.BAD_REQUEST);
  }
}

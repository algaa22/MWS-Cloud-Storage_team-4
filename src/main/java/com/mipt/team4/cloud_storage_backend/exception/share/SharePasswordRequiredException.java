package com.mipt.team4.cloud_storage_backend.exception.share;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class SharePasswordRequiredException extends BaseStorageException {
  public SharePasswordRequiredException(String token) {
    super("Password required for share: " + token, HttpResponseStatus.BAD_REQUEST);
  }
}

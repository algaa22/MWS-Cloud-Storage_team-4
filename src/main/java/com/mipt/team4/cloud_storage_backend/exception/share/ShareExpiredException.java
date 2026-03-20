package com.mipt.team4.cloud_storage_backend.exception.share;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ShareExpiredException extends BaseStorageException {
  public ShareExpiredException(String token) {
    super("Share link has expired: " + token, HttpResponseStatus.BAD_REQUEST);
  }
}

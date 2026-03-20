package com.mipt.team4.cloud_storage_backend.exception.share;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ShareLimitExceededException extends BaseStorageException {
  public ShareLimitExceededException(String token) {
    super("Download limit exceeded for share: " + token, HttpResponseStatus.BAD_REQUEST);
  }
}

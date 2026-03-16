package com.mipt.team4.cloud_storage_backend.exception.netty;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class UnsupportedOperationException extends BaseStorageException {
  public UnsupportedOperationException(String method, String path) {
    super(
        "Unsupported operation: method=%s, path=%s".formatted(method, path),
        HttpResponseStatus.BAD_REQUEST);
  }
}

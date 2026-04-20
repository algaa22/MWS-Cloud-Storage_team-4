package com.mipt.team4.cloud_storage_backend.exception.upload;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class TooSmallFilePartException extends BaseStorageException {

  public TooSmallFilePartException(long minFilePartSize) {
    super(
        "File part must be at least " + minFilePartSize + " bytes in size",
        HttpResponseStatus.BAD_REQUEST);
  }
}

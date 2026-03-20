package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class TooLargeFilePartException extends BaseStorageException {
  public TooLargeFilePartException(long maxFilePartSize) {
    super(
        "File part must be at most " + maxFilePartSize + " bytes in size",
        HttpResponseStatus.BAD_REQUEST);
  }
}

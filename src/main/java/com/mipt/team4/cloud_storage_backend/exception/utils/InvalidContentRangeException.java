package com.mipt.team4.cloud_storage_backend.exception.utils;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class InvalidContentRangeException extends BaseStorageException {
  public InvalidContentRangeException() {
    super(
        "Invalid Range format. Expected 'bytes=start-end', 'bytes=start-' or 'bytes=-suffix'",
        HttpResponseStatus.BAD_REQUEST);
  }
}

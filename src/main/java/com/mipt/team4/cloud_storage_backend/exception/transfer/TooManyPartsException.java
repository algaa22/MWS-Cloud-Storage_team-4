package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class TooManyPartsException extends BaseStorageException {
  public TooManyPartsException(int maxPartsNum) {
    super("Total parts must be at most " + maxPartsNum, HttpResponseStatus.BAD_REQUEST);
  }
}

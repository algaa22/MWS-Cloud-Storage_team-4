package com.mipt.team4.cloud_storage_backend.exception.utils;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class MissingRequiredParamException extends BaseStorageException {
  public MissingRequiredParamException(String name) {
    super("Required parameter " + name + " is missing", HttpResponseStatus.BAD_REQUEST);
  }
}

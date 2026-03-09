package com.mipt.team4.cloud_storage_backend.exception.netty;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class HeaderNotFoundException extends BaseStorageException {

  public HeaderNotFoundException(String headerName) {
    super("Missing required header: " + headerName, HttpResponseStatus.BAD_REQUEST);
  }
}

package com.mipt.team4.cloud_storage_backend.exception.user;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class MissingOldPasswordException extends BaseStorageException {
  public MissingOldPasswordException() {
    super(
        "To change your password, you must enter your old password",
        HttpResponseStatus.BAD_REQUEST);
  }
}

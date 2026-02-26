package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class UploadSessionNotFoundException extends BaseStorageException {

  public UploadSessionNotFoundException(String sessionId) {
    super("Upload session not found: sessionId=" + sessionId, HttpResponseStatus.NOT_FOUND);
  }
}

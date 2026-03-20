package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class MissingUploadContextException extends BaseStorageException {
  public MissingUploadContextException() {
    super("Upload context not found", HttpResponseStatus.CONFLICT);
  }
}

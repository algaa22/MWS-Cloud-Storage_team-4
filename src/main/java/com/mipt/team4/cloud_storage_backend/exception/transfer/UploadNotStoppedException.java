package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class UploadNotStoppedException extends BaseStorageException  {
  public UploadNotStoppedException() {
    super("Cannot resume upload because it is not stopped", HttpResponseStatus.CONFLICT);
  }
}

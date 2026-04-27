package com.mipt.team4.cloud_storage_backend.exception.upload;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class MissingChecksumException extends BaseStorageException {
  public MissingChecksumException() {
    super("The checksum must be specified for antivirus scanning", HttpResponseStatus.BAD_REQUEST);
  }
}

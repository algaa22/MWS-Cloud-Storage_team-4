package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ChecksumMismatchException extends BaseStorageException {
  public ChecksumMismatchException() {
    super("Client/Server checksum mismatch", HttpResponseStatus.BAD_REQUEST);
  }
}

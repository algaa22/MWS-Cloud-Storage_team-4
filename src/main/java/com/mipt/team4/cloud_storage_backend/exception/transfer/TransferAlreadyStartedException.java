package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class TransferAlreadyStartedException extends BaseStorageException {

  public TransferAlreadyStartedException() {
    super("Previous request not completed", HttpResponseStatus.CONFLICT);
  }
}

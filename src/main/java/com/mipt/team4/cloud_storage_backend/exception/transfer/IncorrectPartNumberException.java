package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class IncorrectPartNumberException extends BaseStorageException {
  public IncorrectPartNumberException(int partNumber, int totalParts) {
    super(
        "Part number %s is out of range. Expected value between 1 and %s (totalParts)"
            .formatted(partNumber, totalParts),
        HttpResponseStatus.BAD_REQUEST);
  }
}

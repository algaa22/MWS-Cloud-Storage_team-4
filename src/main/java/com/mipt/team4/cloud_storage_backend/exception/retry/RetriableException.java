package com.mipt.team4.cloud_storage_backend.exception.retry;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;

@Getter
public class RetriableException extends BaseStorageException {
  public RetriableException(String operationName, Throwable cause) {
    super(
        "An error occurred during the operation \"" + operationName + "\". Please retry.",
        cause,
        HttpResponseStatus.CONFLICT);
  }
}

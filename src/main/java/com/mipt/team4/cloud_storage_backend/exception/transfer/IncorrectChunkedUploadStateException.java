package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.controller.storage.chunked.ChunkedUploadState;
import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class IncorrectChunkedUploadStateException extends BaseStorageException {
  public IncorrectChunkedUploadStateException(
      ChunkedUploadState expected, ChunkedUploadState actual) {
    super(
        "Incorrect chunked upload state for called operation: expected=%s, actual=%s"
            .formatted(expected.name(), actual.name()),
        HttpResponseStatus.CONFLICT);
  }
}

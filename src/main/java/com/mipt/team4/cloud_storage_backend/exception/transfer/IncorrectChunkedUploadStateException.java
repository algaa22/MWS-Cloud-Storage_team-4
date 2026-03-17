package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.controller.storage.chunked.ChunkedUploadState;
import com.mipt.team4.cloud_storage_backend.controller.storage.chunked.ChunkedUploadState.Status;
import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class IncorrectChunkedUploadStateException extends BaseStorageException {
  public IncorrectChunkedUploadStateException(
      Status expectedStatus, ChunkedUploadState uploadState) {
    super(
        "Incorrect chunked upload state for called operation: expected=%s, actual=%s"
            .formatted(
                expectedStatus.name(), uploadState != null ? uploadState.getStatus() : Status.IDLE),
        HttpResponseStatus.CONFLICT);
  }
}

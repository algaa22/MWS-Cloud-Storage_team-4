package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.ChunkedUploadStatus;
import io.netty.handler.codec.http.HttpResponseStatus;

public class IncorrectUploadStatusException extends BaseStorageException {
  public IncorrectUploadStatusException(ChunkedUploadStatus expected, ChunkedUploadStatus actual) {
    super(
        "Incorrect upload status: expected=%s, actual=%s".formatted(expected, actual),
        HttpResponseStatus.CONFLICT);
  }
}

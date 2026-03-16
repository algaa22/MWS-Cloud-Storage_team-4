package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.exception.retry.CompleteUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ProcessUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;

@ResponseStatus(409)
public record UploadRetryResponse(
    @ResponseBodyParam("action") String action,
    @ResponseBodyParam("message") String message,
    @ResponseBodyParam("currentFileSize") Long currentFileSize,
    @ResponseBodyParam("partNum") Integer partNum) {
  public UploadRetryResponse(ProcessUploadRetriableException exception) {
    this(
        "RESUME_CONTINUE",
        exception.getMessage(),
        exception.getCurrentFileSize(),
        exception.getPartNum());
  }

  public UploadRetryResponse(CompleteUploadRetriableException exception) {
    this("RESUME_RETRY", exception.getMessage(), null, null);
  }
}

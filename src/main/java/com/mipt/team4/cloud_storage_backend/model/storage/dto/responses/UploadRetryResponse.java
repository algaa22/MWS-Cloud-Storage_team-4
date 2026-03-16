package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.exception.retry.CompleteUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ProcessUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import org.apache.hc.core5.http.HttpStatus;

@ResponseStatus(HttpStatus.SC_CONFLICT)
public record UploadRetryResponse(
    @ResponseBodyParam String action,
    @ResponseBodyParam String message,
    @ResponseBodyParam Long currentFileSize,
    @ResponseBodyParam Integer partNum) {
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

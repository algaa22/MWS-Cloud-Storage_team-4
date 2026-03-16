package com.mipt.team4.cloud_storage_backend.model.common.dto.responses;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;

public record SuccessResponse(
    @ResponseBodyParam("message") String message, @ResponseBodyParam("success") boolean success) {
  public SuccessResponse(String message) {
    this(message, true);
  }
}

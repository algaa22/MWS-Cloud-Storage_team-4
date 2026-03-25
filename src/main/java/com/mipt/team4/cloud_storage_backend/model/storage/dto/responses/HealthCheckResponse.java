package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.ComponentStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.MemoryStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.OverallStatus;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import io.netty.handler.codec.http.HttpResponseStatus;

public record HealthCheckResponse(
    @JsonIgnore @ResponseStatus HttpResponseStatus httpStatus,
    @ResponseBodyParam OverallStatus status,
    @ResponseBodyParam ComponentStatus database,
    @ResponseBodyParam ComponentStatus s3,
    @ResponseBodyParam MemoryStatus memory,
    @ResponseBodyParam String timestamp) {
  @Override
  public OverallStatus status() {
    return status;
  }
}

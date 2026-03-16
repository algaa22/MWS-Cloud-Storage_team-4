package com.mipt.team4.cloud_storage_backend.model.common.dto.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import io.netty.handler.codec.http.HttpResponseStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @JsonIgnore @ResponseStatus HttpResponseStatus status,
    @ResponseBodyParam("message") String message,
    @ResponseBodyParam("success") boolean success,
    @ResponseBodyParam("details") Object details) {
  public ErrorResponse(HttpResponseStatus status, String message) {
    this(status, message, false, null);
  }

  public ErrorResponse(HttpResponseStatus status, String message, Object details) {
    this(status, message, false, details);
  }
}

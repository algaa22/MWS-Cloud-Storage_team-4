package com.mipt.team4.cloud_storage_backend.model.user.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.user.dto.TokenPairDto;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import io.netty.handler.codec.http.HttpResponseStatus;

public record TokenPairResponse(
    @ResponseStatus HttpResponseStatus status,
    @ResponseBodyParam String message,
    @ResponseBodyParam String accessToken,
    @ResponseBodyParam String refreshToken) {
  public TokenPairResponse(HttpResponseStatus status, String message, TokenPairDto tokens) {
    this(status, message, tokens.accessToken(), tokens.refreshToken());
  }
}

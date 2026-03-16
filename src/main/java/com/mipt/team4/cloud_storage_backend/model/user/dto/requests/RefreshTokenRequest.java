package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import jakarta.validation.constraints.NotBlank;

@RequestMapping(method = "POST", path = ApiEndpoints.AUTH_REFRESH)
public record RefreshTokenRequest(
    @RequestHeader("X-Refresh-Token") @NotBlank String refreshToken) {}

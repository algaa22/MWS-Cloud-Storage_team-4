package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.AUTH_LOGOUT)
public record LogoutRequest(@NotNull @RequestHeader String authToken, @UserId UUID userId) {}

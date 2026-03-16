package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationPatterns;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record UpdateUserInfoRequest(
    @UserId UUID userId,
    @RequestHeader(required = false) String oldPassword,
    @Pattern(
            regexp = ValidationPatterns.PASSWORD_REGEXP,
            message = ValidationPatterns.PASSWORD_ERROR)
        @RequestHeader(required = false)
        String newPassword,
    @QueryParam(required = false) String newName) {}

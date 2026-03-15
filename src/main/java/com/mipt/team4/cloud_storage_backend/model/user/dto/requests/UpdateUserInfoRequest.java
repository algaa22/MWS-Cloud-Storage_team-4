package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationPatterns;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.UserId;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record UpdateUserInfoRequest(
    @UserId UUID userId,
    @RequestHeader(value = "X-Old-Password", required = false) String oldPassword,
    @Pattern(
            regexp = ValidationPatterns.PASSWORD_REGEXP,
            message = ValidationPatterns.PASSWORD_ERROR)
        @RequestHeader(value = "X-New-Password", required = false)
        String newPassword,
    @QueryParam(value = "newName", required = false) String newUsername) {}

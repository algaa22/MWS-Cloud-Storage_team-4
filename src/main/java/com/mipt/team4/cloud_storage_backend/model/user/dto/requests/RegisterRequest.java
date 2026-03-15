package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationConstants;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestMapping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RequestMapping(method = "POST", path = ApiEndpoints.AUTH_REGISTER)
public record RegisterRequest(
    @RequestHeader("X-Auth-Email")
        @Pattern(
            regexp = ValidationConstants.EMAIL_REGEXP,
            message = ValidationConstants.EMAIL_ERROR)
        String email,
    @RequestHeader("X-Auth-Password")
        @Pattern(
            regexp = ValidationConstants.PASSWORD_REGEXP,
            message = ValidationConstants.PASSWORD_ERROR)
        String password,
    @RequestHeader("X-Auth-Username") @NotBlank String username) {}

package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationPatterns;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import jakarta.validation.constraints.Pattern;

@RequestMapping(method = "POST", path = ApiEndpoints.AUTH_LOGIN)
public record LoginRequest(
    @RequestHeader("X-Auth-Email")
        @Pattern(regexp = ValidationPatterns.EMAIL_REGEXP, message = ValidationPatterns.EMAIL_ERROR)
        String email,
    @RequestHeader("X-Auth-Password")
        @Pattern(
            regexp = ValidationPatterns.PASSWORD_REGEXP,
            message = ValidationPatterns.PASSWORD_ERROR)
        String password) {}

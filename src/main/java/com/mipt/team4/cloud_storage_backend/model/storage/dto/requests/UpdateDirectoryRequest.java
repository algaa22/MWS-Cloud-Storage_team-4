package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationPatterns;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.DIRECTORIES_PREFIX)
public record UpdateDirectoryRequest(
    @UserId UUID userId,
    @NotNull @QueryParam UUID id,
    @Pattern(
            regexp = ValidationPatterns.FILE_NAME_REGEXP,
            message = ValidationPatterns.FILE_NAME_ERROR)
        @QueryParam(required = false)
        String newName,
    @QueryParam(required = false) UUID newParentId) {}

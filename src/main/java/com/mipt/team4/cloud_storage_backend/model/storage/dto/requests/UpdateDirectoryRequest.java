package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationConstants;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.UserId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.DIRECTORIES_PREFIX)
public record UpdateDirectoryRequest(
    @UserId UUID userId,
    @NotNull @QueryParam("id") UUID directoryId,
    @Pattern(
            regexp = ValidationConstants.FILE_NAME_REGEXP,
            message = ValidationConstants.FILE_NAME_ERROR)
        @QueryParam("newName")
        String newName,
    @QueryParam("newParentId") UUID newParentId) {}

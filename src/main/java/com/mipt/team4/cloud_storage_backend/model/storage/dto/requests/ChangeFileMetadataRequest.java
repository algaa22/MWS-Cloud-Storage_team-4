package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationPatterns;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

@RequestMapping(method = "PUT", path = ApiEndpoints.FILES_PREFIX)
public record ChangeFileMetadataRequest(
    @UserId UUID userId,
    @NotNull @QueryParam("id") UUID fileId,
    @Pattern(
            regexp = ValidationPatterns.FILE_NAME_REGEXP,
            message = ValidationPatterns.FILE_NAME_ERROR)
        @QueryParam(value = "newName", required = false)
        String newName,
    @QueryParam(value = "newParentId", required = false) UUID newParentId,
    @RequestHeader(value = "X-File-New-Visibility", required = false) String visibility,
    @RequestHeader(value = "X-File-New-Tags", required = false)
        List<
                @Pattern(
                    regexp = ValidationPatterns.SINGLE_TAG_REGEXP,
                    message = ValidationPatterns.SINGLE_TAG_ERROR)
                String>
            tags) {}

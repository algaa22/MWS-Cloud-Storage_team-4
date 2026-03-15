package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationConstants;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.UserId;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.FILES_UPLOAD)
public record StartChunkedUploadRequest(
    @UserId UUID userId,
    @Pattern(
            regexp = ValidationConstants.FILE_NAME_REGEXP,
            message = ValidationConstants.FILE_NAME_ERROR)
        @QueryParam("name")
        String name,
    @QueryParam("parentId") UUID parentId,
    @RequestHeader("X-File-Size") @Positive long fileSize,
    @RequestHeader(value = "X-File-Tags", required = false)
        List<
                @Pattern(
                    regexp = ValidationConstants.SINGLE_TAG_REGEXP,
                    message = ValidationConstants.SINGLE_TAG_ERROR)
                String>
            tags) {}

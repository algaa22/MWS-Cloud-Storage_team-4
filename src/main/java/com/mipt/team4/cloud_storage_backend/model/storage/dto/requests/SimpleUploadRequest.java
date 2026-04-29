package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.config.constants.netty.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.config.constants.netty.ValidationPatterns;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestBody;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.FILES_SIMPLE_UPLOAD)
public record SimpleUploadRequest(
    @UserId UUID userId,
    @Size(min = 1, max = 100) // TODO: cfg
        @Pattern(
            regexp = ValidationPatterns.FILE_NAME_REGEXP,
            message = ValidationPatterns.FILE_NAME_ERROR)
        @QueryParam
        String name,
    @QueryParam(required = false) UUID parentId,
    @RequestHeader(value = "X-File-Tags", required = false)
        List<
                @Pattern(
                    regexp = ValidationPatterns.SINGLE_TAG_REGEXP,
                    message = ValidationPatterns.SINGLE_TAG_ERROR)
                String>
            tags,
    @RequestHeader(value = "Content-SHA256", required = false) String checksum,
    @NotEmpty @RequestBody byte[] data) {}

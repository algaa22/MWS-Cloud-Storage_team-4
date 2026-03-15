package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationPatterns;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.FILES_UPLOAD)
public record FileUploadRequest(
    @UserId UUID userId,
    @NotBlank
        @Pattern(
            regexp = ValidationPatterns.FILE_NAME_REGEXP,
            message = ValidationPatterns.FILE_NAME_ERROR)
        @QueryParam("name")
        String name,
    @QueryParam(value = "parentId", required = false) UUID parentId,
    @RequestHeader(value = "X-File-Tags", required = false)
        List<
                @Pattern(
                    regexp = ValidationPatterns.SINGLE_TAG_REGEXP,
                    message = ValidationPatterns.SINGLE_TAG_ERROR)
                String>
            tags,
    @Positive @RequestHeader("X-File-Size") long size,
    @NotEmpty @RequestBodyParam byte[] data) {}

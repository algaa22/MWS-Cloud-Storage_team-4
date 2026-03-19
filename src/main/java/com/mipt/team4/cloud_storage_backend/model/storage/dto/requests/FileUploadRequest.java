package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationPatterns;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestBody;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.FILES_SIMPLE_UPLOAD)
public record FileUploadRequest(
    @UserId UUID userId,
    @NotBlank
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
    @RequestHeader(value = "Content-MD5", required = false) String checksum,
    @NotEmpty @RequestBody byte[] data) {}

package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.config.constants.netty.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.FILES_CHUNKED_UPLOAD_PART)
public record ChunkedUploadPartRequest(
    @UserId UUID userId,
    @NotNull @QueryParam UUID sessionId,
    @Positive @QueryParam int part,
    @Positive @RequestHeader("Content-Length") long partSize,
    @RequestHeader(value = "Content-SHA256", required = false) String checksum) {}

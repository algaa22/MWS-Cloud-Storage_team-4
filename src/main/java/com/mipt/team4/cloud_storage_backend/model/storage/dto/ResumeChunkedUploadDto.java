package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import jakarta.validation.constraints.NotNull;

@RequestMapping(method = "POST", path = ApiEndpoints.FILES_UPLOAD)
public record ResumeChunkedUploadDto(@NotNull @RequestHeader("sessionId") String sessionId) {}

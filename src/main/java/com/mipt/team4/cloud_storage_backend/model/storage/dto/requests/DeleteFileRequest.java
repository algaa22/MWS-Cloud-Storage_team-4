package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@RequestMapping(method = "DELETE", path = ApiEndpoints.FILES_PREFIX) // Путь /api/files/
public record DeleteFileRequest(
    @UserId UUID userId,
    @QueryParam(value = "permanent", defaultValue = "false") boolean permanent,
    @NotNull @QueryParam("id") UUID fileId) {}

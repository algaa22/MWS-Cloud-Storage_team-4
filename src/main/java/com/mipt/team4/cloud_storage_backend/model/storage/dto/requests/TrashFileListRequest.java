package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.config.constants.netty.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.NestedDto;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import java.util.UUID;

@RequestMapping(method = "GET", path = ApiEndpoints.FILES_TRASH)
public record TrashFileListRequest(
    @UserId UUID userId,
    @QueryParam(required = false) UUID parentId,
    @NestedDto FilePaginationParams pagination) {}

package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationPatterns;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.NestedDto;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

@RequestMapping(method = "GET", path = ApiEndpoints.FILES_LIST)
public record GetFileListRequest(
    @UserId UUID userId,
    @QueryParam(required = false) UUID parentId,
    @QueryParam(defaultValue = "false", required = false) boolean recursive,
    @QueryParam(defaultValue = "false", required = false) boolean includeDirectories,
    @NestedDto FilePaginationParams pagination,
    @QueryParam(required = false)
        List<
                @Pattern(
                    regexp = ValidationPatterns.SINGLE_TAG_REGEXP,
                    message = ValidationPatterns.SINGLE_TAG_ERROR)
                String>
            tags) {}

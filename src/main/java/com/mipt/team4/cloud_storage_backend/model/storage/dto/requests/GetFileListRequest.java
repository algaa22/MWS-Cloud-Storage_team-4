package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationConstants;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.UserId;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

@RequestMapping(method = "GET", path = ApiEndpoints.FILES_LIST)
public record GetFileListRequest(
    @UserId UUID userId,
    @QueryParam(value = "parentId", required = false) UUID parentId,
    @QueryParam(value = "recursive", defaultValue = "false", required = false) boolean recursive,
    @QueryParam(value = "includeDirectories", defaultValue = "false", required = false)
        boolean includeDirectories,
    @QueryParam(value = "tags", required = false)
        List<
                @Pattern(
                    regexp = ValidationConstants.SINGLE_TAG_REGEXP,
                    message = ValidationConstants.SINGLE_TAG_ERROR)
                String>
            tags) {}

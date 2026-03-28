package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.model.common.dto.requests.CommonPaginationParams;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileSortBy;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.NestedDto;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import jakarta.validation.constraints.NotBlank;

public record FilePaginationParams(
    @NestedDto CommonPaginationParams commonParams,
    @NotBlank @QueryParam(defaultValue = FileSortBy.DEFAULT_VALUE) FileSortBy sortBy) {}

package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.model.common.dto.requests.CommonPaginationParams;
import com.mipt.team4.cloud_storage_backend.model.user.enums.UserSortBy;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.NestedDto;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import jakarta.validation.constraints.NotBlank;

public record UserPaginationParams(
    @NestedDto CommonPaginationParams commonParams, @NotBlank @QueryParam UserSortBy sortBy) {}

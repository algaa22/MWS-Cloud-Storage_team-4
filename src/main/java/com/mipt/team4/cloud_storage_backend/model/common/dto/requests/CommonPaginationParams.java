package com.mipt.team4.cloud_storage_backend.model.common.dto.requests;

import com.mipt.team4.cloud_storage_backend.model.common.enums.PaginationDirection;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.Range;

public record CommonPaginationParams(
    @PositiveOrZero @QueryParam int page,
    @Range(min = 1, max = 100) @QueryParam(defaultValue = "20") int size,
    @NotBlank @QueryParam(defaultValue = PaginationDirection.DEFAULT_VALUE)
        PaginationDirection direction) {}

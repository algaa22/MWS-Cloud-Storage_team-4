package com.mipt.team4.cloud_storage_backend.model.common.dto.responses;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.util.List;

public record PageResponse<T>(
    @ResponseBodyParam List<T> content,
    @ResponseBodyParam long totalElements,
    @ResponseBodyParam int totalPages,
    @ResponseBodyParam int page,
    @ResponseBodyParam int size) {}

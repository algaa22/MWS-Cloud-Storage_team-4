package com.mipt.team4.cloud_storage_backend.model.common.dto.responses;

import java.util.List;

public record PageResponse<T>(
    List<T> content, long totalElements, int totalPages, int page, int size) {}

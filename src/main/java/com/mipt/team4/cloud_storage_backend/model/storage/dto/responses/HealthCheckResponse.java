package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.storage.enums.ComponentStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.MemoryStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.OverallStatus;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;

public record HealthCheckResponse(
    @ResponseBodyParam OverallStatus status,
    @ResponseBodyParam ComponentStatus database,
    @ResponseBodyParam ComponentStatus s3,
    @ResponseBodyParam MemoryStatus memory) {}

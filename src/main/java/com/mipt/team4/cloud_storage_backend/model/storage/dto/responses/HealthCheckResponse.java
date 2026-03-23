package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;

public record HealthCheckResponse(
    @ResponseBodyParam String status,
    @ResponseBodyParam String database,
    @ResponseBodyParam String s3,
    @ResponseBodyParam String memory) {}

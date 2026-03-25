package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.storage.enums.HealthComponentStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.HealthMemoryStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.HealthOverallStatus;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import io.netty.handler.codec.http.HttpResponseStatus;

public record HealthCheckResponse(
    @ResponseStatus HttpResponseStatus httpStatus,
    @ResponseBodyParam HealthOverallStatus status,
    @ResponseBodyParam HealthComponentStatus database,
    @ResponseBodyParam HealthComponentStatus s3,
    @ResponseBodyParam HealthMemoryStatus memory,
    @ResponseBodyParam String timestamp) {}

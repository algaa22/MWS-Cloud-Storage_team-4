package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.config.constants.netty.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;

@RequestMapping(method = "GET", path = ApiEndpoints.HEALTHCHECK)
public record HealthCheckRequest() {}

package com.mipt.team4.cloud_storage_backend.model.common.dto.responses;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import java.util.UUID;

@ResponseStatus(201)
public record CreatedResponse(
    @ResponseBodyParam("id") UUID id, @ResponseBodyParam("message") String message) {}

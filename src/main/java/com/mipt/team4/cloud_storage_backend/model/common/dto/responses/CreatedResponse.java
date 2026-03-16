package com.mipt.team4.cloud_storage_backend.model.common.dto.responses;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import java.util.UUID;
import org.apache.hc.core5.http.HttpStatus;

@ResponseStatus(HttpStatus.SC_CREATED)
public record CreatedResponse(@ResponseBodyParam UUID id, @ResponseBodyParam String message) {}

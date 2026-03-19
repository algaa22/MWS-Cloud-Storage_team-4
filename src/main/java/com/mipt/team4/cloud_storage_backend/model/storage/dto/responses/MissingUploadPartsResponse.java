package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import java.util.UUID;
import org.apache.hc.core5.http.HttpStatus;

@ResponseStatus(HttpStatus.SC_CONFLICT)
public record MissingUploadPartsResponse(
    @ResponseBodyParam String message,
    @ResponseBodyParam UUID sessionId,
    @ResponseBodyParam String missingPartsBitmask) {}

package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.storage.enums.ChunkedUploadStatus;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import java.util.UUID;

public record UploadStatusResponse(
    @ResponseBodyParam UUID sessionId,
    @ResponseBodyParam ChunkedUploadStatus status,
    @ResponseBodyParam long currentSize,
    @ResponseBodyParam long currentParts,
    @ResponseBodyParam String missingPartsBitmask) {}

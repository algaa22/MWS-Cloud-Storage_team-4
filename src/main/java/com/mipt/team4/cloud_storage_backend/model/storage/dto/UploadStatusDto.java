package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.model.storage.enums.ChunkedUploadStatus;
import java.util.BitSet;
import java.util.UUID;

public record UploadStatusDto(
    UUID sessionId,
    ChunkedUploadStatus status,
    long currentSize,
    long currentParts,
    BitSet missingPartsBitset) {}

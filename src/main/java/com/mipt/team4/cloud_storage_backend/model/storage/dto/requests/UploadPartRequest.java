package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import java.util.UUID;

public record UploadPartRequest(
    String uploadId, UUID userId, UUID fileId, int partIndex, byte[] bytes) {}

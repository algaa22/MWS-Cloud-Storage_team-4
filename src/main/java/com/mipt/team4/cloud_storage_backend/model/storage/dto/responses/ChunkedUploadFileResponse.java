package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import java.util.UUID;

public record ChunkedUploadFileResponse(UUID fileId, long fileSize, long totalParts) {}

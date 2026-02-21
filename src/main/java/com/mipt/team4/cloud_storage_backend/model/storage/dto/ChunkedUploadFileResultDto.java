package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.util.UUID;

public record ChunkedUploadFileResultDto(
    UUID parentId, String name, long fileSize, long totalParts) {}

package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.io.InputStream;
import java.util.UUID;

public record ChunkedUploadPartDto(
    UUID sessionId,
    UUID userId,
    int partNumber,
    String checksum,
    long size,
    InputStream inputStream) {}

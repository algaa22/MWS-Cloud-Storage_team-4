package com.mipt.team4.cloud_storage_backend.model.storage.dto;

public record ChunkedUploadFileResultDto(String filePath, long fileSize, long totalParts) {}

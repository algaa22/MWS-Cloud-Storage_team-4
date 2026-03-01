package com.mipt.team4.cloud_storage_backend.model.storage.dto;

public record ChunkedUploadFileResult(String filePath, long fileSize, long totalParts) {}

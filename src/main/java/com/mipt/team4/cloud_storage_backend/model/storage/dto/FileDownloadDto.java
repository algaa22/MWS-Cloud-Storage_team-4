package com.mipt.team4.cloud_storage_backend.model.storage.dto;

public record FileDownloadDto(String filePath, String mimeType, byte[] data) {}

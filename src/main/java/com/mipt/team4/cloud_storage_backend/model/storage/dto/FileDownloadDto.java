package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.io.InputStream;

// TODO: naming
public record FileDownloadDto(String filePath, String mimeType, InputStream fileStream, long size) {}

package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.io.InputStream;

public record FileDownloadInfoDto(
    String mimeType, InputStream stream, ContentRangeDto range, long totalSize) {}

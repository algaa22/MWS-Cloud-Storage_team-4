package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.io.InputStream;

public record FileDownloadDto(String path, String mimeType, InputStream stream, long size) {}

package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.io.InputStream;
import java.util.UUID;

public record FileDownloadDto(
    UUID parentId, String name, String mimeType, InputStream stream, long size) {}

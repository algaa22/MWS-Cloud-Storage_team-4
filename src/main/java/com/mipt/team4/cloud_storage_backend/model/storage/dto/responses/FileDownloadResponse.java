package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import java.io.InputStream;
import java.util.UUID;

public record FileDownloadResponse(String mimeType, InputStream stream, long size) {}

package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestBodyParam;
import java.util.UUID;

public record StartChunkedUploadResponse(@RequestBodyParam UUID sessionId) {}

package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import io.netty.buffer.CompositeByteBuf;
import java.util.UUID;

public record ChunkedUploadPartContext(
    UUID sessionId, UUID userId, int partNumber, CompositeByteBuf accumulator) {}

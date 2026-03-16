package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.util.UUID;

public record UploadChunkDto(UUID sessionId, byte[] chunkData) {}

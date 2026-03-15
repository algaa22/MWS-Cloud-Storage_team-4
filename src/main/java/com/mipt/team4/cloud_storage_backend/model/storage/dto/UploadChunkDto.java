package com.mipt.team4.cloud_storage_backend.model.storage.dto;

public record UploadChunkDto(String sessionId, byte[] chunkData) {}

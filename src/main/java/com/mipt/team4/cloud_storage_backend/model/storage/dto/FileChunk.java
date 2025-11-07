package com.mipt.team4.cloud_storage_backend.model.storage.dto;

public record FileChunk(String sessionId, String path, int chunkIndex, byte[] chunkData) {}

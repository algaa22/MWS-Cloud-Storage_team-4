package com.mipt.team4.cloud_storage_backend.model.storage.dto;

public record FileChunkDto(String sessionId, int chunkIndex, byte[] chunkData) {}

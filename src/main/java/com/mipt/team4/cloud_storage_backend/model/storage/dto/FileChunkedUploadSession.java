package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.util.List;

public record FileChunkedUploadSession(
    String sessionId,
    String ownerId,
    long totalFileSize,
    int totalChunks,
    String path,
    List<String> tags,
    List<FileChunk> chunks) {}

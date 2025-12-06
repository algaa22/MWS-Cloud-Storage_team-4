package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.util.List;
import java.util.UUID;

public record StorageDto(
    UUID storageId,
    UUID userId,
    String path,
    String type,
    String visibility,
    long size,
    boolean isDeleted,
    List<String> tags,
    boolean isDirectory) {}

package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import java.util.List;
import java.util.UUID;

public record FileDto(
    UUID id,
    UUID ownerId,
    String name,
    String path,
    String bucketName,
    String url,
    String type,
    long size,
    String visibility,
    boolean isActive,
    List<String> tags) {}

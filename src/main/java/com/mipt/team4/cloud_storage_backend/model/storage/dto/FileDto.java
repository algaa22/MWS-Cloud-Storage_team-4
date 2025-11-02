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
    String visibility,
    long size,
    boolean isDeleted,
    List<String> tags) {}

package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
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
    boolean isDirectory) {

  public StorageDto(StorageEntity entity) {
    this(
        entity.getEntityId(),
        entity.getUserId(),
        entity.getPath(),
        entity.getMimeType(),
        entity.getVisibility(),
        entity.getSize(),
        entity.isDeleted(),
        entity.getTags(),
        entity.isDirectory());
  }
}

package com.mipt.team4.cloud_storage_backend.model.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileInfo;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;

public class FileMapper {
  public static FileEntity toEntity(FileInfo dto) {
    return new FileEntity(
        dto.id(),
        dto.ownerId(),
        dto.path(),
        dto.type(),
        dto.visibility(),
        dto.size(),
        dto.isDeleted(),
        dto.tags());
  }

  public static FileInfo toDto(FileEntity entity) {
    return new FileInfo(
        entity.getId(),
        entity.getOwnerId(),
        entity.getStoragePath(),
        entity.getMimeType(),
        entity.getVisibility(),
        entity.getSize(),
        entity.isDeleted(),
        entity.getTags());
  }
}

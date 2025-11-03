package com.mipt.team4.cloud_storage_backend.model.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;

public class FileMapper {
  public static FileEntity toEntity(FileDto dto) {
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

  public static FileDto toDto(FileEntity entity) {
    return new FileDto(
        entity.getId(),
        entity.getOwnerId(),
        entity.getPath(),
        entity.getType(),
        entity.getVisibility(),
        entity.getSize(),
        entity.isDeleted(),
        entity.getTags());
  }
}

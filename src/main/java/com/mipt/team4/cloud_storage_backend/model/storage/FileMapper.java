package com.mipt.team4.cloud_storage_backend.model.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;

public class FileMapper {
  public static FileEntity toEntity(FileDto dto) {
    return new FileEntity(
        dto.id(), dto.ownerId(), dto.name(), dto.path(), dto.bucketName(), dto.url(), dto.type(), dto.size(), dto.visibility(), dto.isActive(), dto.tags());
  }

  public static FileDto toDto(FileEntity entity) {
    return new FileDto(
        entity.getId(),
        entity.getOwnerId(),
        entity.getName(),
        entity.getPath(),
        entity.getBucketName(),
        entity.getUrl(),
        entity.getType(),
        entity.getSize(),
        entity.getVisibility(),
        entity.isActive(),
        entity.getTags());
  }
}

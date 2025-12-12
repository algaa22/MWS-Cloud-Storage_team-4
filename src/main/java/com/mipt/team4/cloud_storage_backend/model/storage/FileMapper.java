package com.mipt.team4.cloud_storage_backend.model.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;

// TODO: зачем мапперы
public class FileMapper {
  public static StorageEntity toEntity(StorageDto dto) {
    return new StorageEntity(
        dto.storageId(),
        dto.userId(),
        dto.path(),
        dto.type(),
        dto.visibility(),
        dto.size(),
        dto.isDeleted(),
        dto.tags(),
        dto.isDirectory());
  }

  public static StorageDto toDto(StorageEntity entity) {
    return new StorageDto(
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

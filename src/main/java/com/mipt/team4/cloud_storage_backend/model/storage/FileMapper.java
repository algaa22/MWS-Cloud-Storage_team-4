package com.mipt.team4.cloud_storage_backend.model.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;

public class FileMapper {
  public static StorageEntity toEntity(FileDto dto) {
    return new StorageEntity(
        dto.fileId(),
        dto.userId(),
        dto.path(),
        dto.type(),
        dto.visibility(),
        dto.size(),
        dto.isDeleted(),
        dto.tags(),
        dto.isDirectory());
  }

  public static FileDto toDto(StorageEntity entity) {
    return new FileDto(
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

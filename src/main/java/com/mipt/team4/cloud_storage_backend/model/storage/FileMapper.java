package com.mipt.team4.cloud_storage_backend.model.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;

public class FileMapper {
  public static FileEntity toEntity(FileDto dto) {
    return new FileEntity(
        dto.name(), dto.path(), dto.bucketName(), dto.url(), dto.type(), dto.size(), dto.tags());
  }

  public static FileDto toDto(FileEntity entity) {
    return new FileDto(
        entity.getName(),
        entity.getPath(),
        entity.getBucketName(),
        entity.getUrl(),
        entity.getType(),
        entity.getSize(),
        entity.getTags());
  }
}

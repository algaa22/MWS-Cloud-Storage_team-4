package com.mipt.team4.cloud_storage_backend.model.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;

public class FileMapper {
  public static FileEntity toEntity(FileDto dto) {
    return new FileEntity(
        dto.fileId(),
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
        entity.getFileId(),
        entity.getOwnerId(),
        StoragePaths.getFilePathFromS3Key(entity.getS3Key()),
        entity.getMimeType(),
        entity.getVisibility(),
        entity.getSize(),
        entity.isDeleted(),
        entity.getTags());
  }
}

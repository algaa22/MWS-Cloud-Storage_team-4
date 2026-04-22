package com.mipt.team4.cloud_storage_backend.antivirus.model.mapper;

import com.mipt.team4.cloud_storage_backend.antivirus.model.dto.ScanTaskDto;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;

public class ScanTaskMapper {
  public static ScanTaskDto toTask(StorageEntity entity) {
    return new ScanTaskDto(
        entity.getId(),
        entity.getName(),
        entity.getHash(),
        entity.getMimeType(),
        entity.getS3Key(),
        entity.getSize());
  }
}

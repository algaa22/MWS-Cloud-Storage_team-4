package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.utils.converter.StringListConverter;
import java.util.UUID;

public record FileInfoResponse(
    @ResponseBodyParam UUID id,
    @ResponseBodyParam UUID parentId,
    @ResponseBodyParam String name,
    @ResponseBodyParam long size,
    @ResponseBodyParam String tags,
    @ResponseBodyParam String mimeType,
    @ResponseBodyParam String visibility,
    @ResponseBodyParam String updatedAt,
    @ResponseBodyParam boolean isDirectory,
    @ResponseBodyParam boolean isDeleted) {
  public static FileInfoResponse from(StorageEntity entity) {
    return new FileInfoResponse(
        entity.getId(),
        entity.getParentId(),
        entity.getName(),
        entity.getSize(),
        StringListConverter.toString(entity.getTags()),
        entity.getMimeType(),
        entity.getVisibility(),
        entity.getUpdatedAt().toString(),
        entity.isDirectory(),
        entity.isDeleted());
  }
}

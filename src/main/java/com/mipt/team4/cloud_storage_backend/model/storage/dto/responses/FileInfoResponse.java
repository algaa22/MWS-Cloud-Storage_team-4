package com.mipt.team4.cloud_storage_backend.model.storage.dto.responses;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.ResponseBodyParam;
import java.util.List;
import java.util.UUID;

public record FileInfoResponse(
    @ResponseBodyParam("id") UUID id,
    @ResponseBodyParam("parentId") UUID parentId,
    @ResponseBodyParam("name") String name,
    @ResponseBodyParam("size") long size,
    @ResponseBodyParam("tags") List<String> tags,
    @ResponseBodyParam("mimeType") String mimeType,
    @ResponseBodyParam("visibility") String visibility,
    @ResponseBodyParam("updatedAt") String updatedAt,
    @ResponseBodyParam("isDirectory") boolean isDirectory) {
  public static FileInfoResponse from(StorageEntity entity) {
    return new FileInfoResponse(
        entity.getId(),
        entity.getParentId(),
        entity.getName(),
        entity.getSize(),
        entity.getTags(),
        entity.getMimeType(),
        entity.getVisibility(),
        entity.getUpdatedAt().toString(),
        entity.isDirectory());
  }
}

package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
public record StorageDto(
    UUID storageId,
    UUID userId,
    String path,
    String type,
    String visibility,
    long size,
    boolean isDeleted,
    List<String> tags,
    boolean isDirectory,
    FileStatus status,
    FileOperationType operationType,
    LocalDateTime startedAt,
    LocalDateTime updatedAt,
    String errorMessage,
    int retryCount) {

  public StorageDto(StorageEntity entity) {
    this(
        entity.getEntityId(),
        entity.getUserId(),
        entity.getPath(),
        entity.getMimeType(),
        entity.getVisibility(),
        entity.getSize(),
        entity.isDeleted(),
        entity.getTags(),
        entity.isDirectory(),
        entity.getStatus(),
        entity.getOperationType(),
        entity.getStartedAt(),
        entity.getUpdatedAt(),
        entity.getErrorMessage(),
        entity.getRetryCount());
  }
}

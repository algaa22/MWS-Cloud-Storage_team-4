package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileOperationType;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileVisibility;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StorageEntity {
  @EqualsAndHashCode.Include private final UUID id;
  private final UUID userId;

  private final String mimeType;
  private final boolean isDirectory;

  @Builder.Default private String visibility = FileVisibility.PRIVATE.toString();
  @Builder.Default private boolean isDeleted = false;
  private List<String> tags;
  private UUID parentId;
  private String name;
  private long size;

  @Builder.Default private FileStatus status = FileStatus.READY;
  @Builder.Default private int retryCount = 0;
  private FileOperationType operationType;
  private LocalDateTime startedAt;
  private LocalDateTime updatedAt;
  private String errorMessage;

  public String getS3Key() {
    return StoragePaths.getS3Key(userId, id);
  }
}

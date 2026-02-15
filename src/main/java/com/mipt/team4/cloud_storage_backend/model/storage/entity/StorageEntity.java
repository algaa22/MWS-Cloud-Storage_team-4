package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileVisibility;
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
  @EqualsAndHashCode.Include private final UUID entityId;
  private final UUID userId;
  private final String mimeType;
  private final long size;
  private String path;
  @Builder.Default private String visibility = FileVisibility.PRIVATE.toString();
  @Builder.Default private boolean isDeleted = false;
  private boolean isDirectory;
  private List<String> tags;
}

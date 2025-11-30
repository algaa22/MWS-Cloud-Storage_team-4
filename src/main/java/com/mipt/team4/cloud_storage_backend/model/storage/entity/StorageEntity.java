package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class StorageEntity {
  private final UUID entityId;
  private final UUID userId;
  private final String mimeType;
  private final long size;
  private String path;
  private String visibility;
  private boolean isDeleted;
  private boolean isDirectory;
  private List<String> tags;

  public StorageEntity(
      UUID entityId,
      UUID userId,
      String path,
      String mimeType,
      String visibility,
      long size,
      boolean isDeleted,
      List<String> tags,
      boolean isDirectory) {
    this.entityId = entityId;
    this.userId = userId;
    this.size = size;
    this.path = path;
    this.visibility = visibility;
    this.isDeleted = isDeleted;
    this.tags = tags;
    this.mimeType = mimeType;
    this.isDirectory = isDirectory;
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || getClass() != object.getClass()) return false;

    StorageEntity that = (StorageEntity) object;
    return Objects.equals(entityId, that.entityId);
  }

  public boolean fullEquals(Object object) {
    if (object == null || getClass() != object.getClass()) return false;
    StorageEntity that = (StorageEntity) object;
    return size == that.size
        && isDeleted == that.isDeleted
        && isDirectory == that.isDirectory
        && Objects.equals(entityId, that.entityId)
        && Objects.equals(userId, that.userId)
        && Objects.equals(mimeType, that.mimeType)
        && Objects.equals(path, that.path)
        && Objects.equals(visibility, that.visibility)
        && Objects.equals(tags, that.tags);
  }

  public UUID getEntityId() {
    return entityId;
  }

  public UUID getUserId() {
    return userId;
  }

  public long getSize() {
    return size;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean isActive) {
    this.isDeleted = isActive;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getMimeType() {
    return mimeType;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  public void setDirectory(boolean directory) {
    isDirectory = directory;
  }
}

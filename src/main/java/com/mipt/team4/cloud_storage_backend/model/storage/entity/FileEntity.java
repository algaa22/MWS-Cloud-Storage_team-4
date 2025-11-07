package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FileEntity {
  private final String id;
  private final String ownerId;
  private final String mimeType;
  private final long size;
  private String storagePath;
  private String visibility;
  private boolean isDeleted;
  private List<String> tags;

  public FileEntity(
      String id,
      String ownerId,
      String storagePath,
      String mimeType,
      String visibility,
      long size,
      boolean isDeleted,
      List<String> tags) {
    this.id = id;
    this.ownerId = ownerId;
    this.size = size;
    this.storagePath = storagePath;
    this.visibility = visibility;
    this.isDeleted = isDeleted;
    this.tags = tags;
    this.mimeType = mimeType;
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || getClass() != object.getClass()) return false;

    FileEntity that = (FileEntity) object;
    return Objects.equals(id, that.id);
  }

  public boolean fullEquals(Object object) {
    if (object == null || getClass() != object.getClass()) return false;

    FileEntity that = (FileEntity) object;
    return size == that.size
        && isDeleted == that.isDeleted
        && Objects.equals(id, that.id)
        && Objects.equals(ownerId, that.ownerId)
        && Objects.equals(mimeType, that.mimeType)
        && Objects.equals(storagePath, that.storagePath)
        && Objects.equals(visibility, that.visibility)
        && Objects.equals(tags, that.tags);
  }

  public String getId() {
    return id;
  }

  public String getOwnerId() {
    return ownerId;
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

  public void setTags(String tags) {
    this.tags = Collections.singletonList(tags);
  }

  public String getStoragePath() {
    return storagePath;
  }

  public void setStoragePath(String storagePath) {
    this.storagePath = storagePath;
  }

  public String getMimeType() {
    return mimeType;
  }
}

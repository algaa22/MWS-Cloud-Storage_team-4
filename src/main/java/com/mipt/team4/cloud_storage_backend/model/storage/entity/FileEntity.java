package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class FileEntity {
  private final UUID id;
  private final UUID ownerId;
  private final String type;
  private String path;
  private String visibility;
  private final long size;
  private boolean isDeleted;
  private List<String> tags;

  public FileEntity(
      UUID id,
      UUID ownerId,
      String path,
      String type,
      String visibility,
      long size,
      boolean isDeleted,
      List<String> tags) {
    this.id = id;
    this.ownerId = ownerId;
    this.size = size;
    this.path = path;
    this.visibility = visibility;
    this.isDeleted = isDeleted;
    this.tags = tags;
    this.type = type;
  }

  public UUID getId() { return id; }

  public UUID getOwnerId() { return ownerId; }

  public long getSize() {
    return size;
  }

  public String getVisibility() { return visibility; }

  public void setVisibility(String visibility) { this.visibility = visibility; }

  public boolean isDeleted() { return isDeleted; }

  public void setDeleted(boolean isActive) { this.isDeleted = isActive; }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = Collections.singletonList(tags);
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getType() {
    return type;
  }
}

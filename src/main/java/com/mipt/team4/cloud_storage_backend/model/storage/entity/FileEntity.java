package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class FileEntity {
  private final UUID id;
  private final UUID ownerId;
  private final long size;
  private final String type;
  private String name;
  private String path;
  private String url;
  private String visibility;
  private boolean isActive;
  private List<String> tags;
  private String bucketName;

  public FileEntity(
      UUID id, UUID ownerId, String name,
      String path,
      String bucketName,
      String url,
      String type,
      long size,
      String visibility,
      boolean isActive,
      List<String> tags) {
    this.id = id;
    this.ownerId = ownerId;
    this.name = name;
    this.size = size;
    this.path = path;
    this.url = url;
    this.visibility = visibility;
    this.isActive = isActive;
    this.tags = tags;
    this.type = type;
    this.bucketName = bucketName;
  }

  public UUID getId() { return id; }

  public UUID getOwnerId() { return ownerId; }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getSize() {
    return size;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getVisibility() { return visibility; }

  public void setVisibility(String visibility) { this.visibility = visibility; }

  public boolean isActive() { return isActive; }

  public void setActive(boolean isActive) { this.isActive = isActive; }

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

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public String getType() {
    return type;
  }
}

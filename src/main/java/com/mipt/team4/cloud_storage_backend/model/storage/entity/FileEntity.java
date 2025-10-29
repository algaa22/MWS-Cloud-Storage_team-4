package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import java.util.Collections;
import java.util.List;

public class FileEntity {
  private final long size;
  private final String type;
  private String name;
  private String path;
  private String url;
  private List<String> tags;
  private String bucketName;

  public FileEntity(
      String name,
      String path,
      String bucketName,
      String url,
      String type,
      long size,
      List<String> tags) {
    this.name = name;
    this.size = size;
    this.path = path;
    this.url = url;
    this.tags = tags;
    this.type = type;
    this.bucketName = bucketName;
  }

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

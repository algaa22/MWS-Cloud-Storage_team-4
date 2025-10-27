package entity;

import java.util.Collections;
import java.util.UUID;
import java.util.List;

public class FileEntity {
  private String name;
  private final long size;
  private String path;
  private String url;
  private List<String> tags;
  private final String type;
  private String key;
  private String bucketName;

  public FileEntity(String name, long size, String path, String url, List<String> tags, String type,  String key, String bucketName) {
    this.name = name;
    this.size = size;
    this.path = path;
    this.url = url;
    this.tags = tags;
    this.type = type;
    this.key = key;
    this.bucketName = bucketName;
  }

  //геттеры
  public String getName() {
    return name;
  }
  public long getSize() {
    return size;
  }
  public String getUrl() {
    return url;
  }
  public List<String> getTags() {
    return tags;
  }
  public String getPath() {
    return path;
  }
  public String getKey() {
    return key;
  }
  public String getBucketName() {
    return bucketName;
  }
  public String getType() {
    return type;
  }

  //сеттеры
  public void setName(String name) {
    this.name = name;
  }
  public void setPath(String path) {
    this.path = path;
  }
  public void setUrl(String url) {
    this.url = url;
  }
  public void setTags(String tags) {
    this.tags = Collections.singletonList(tags);
  }
  public void setKey(String key) {
    this.key = key;
  }
  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }
}

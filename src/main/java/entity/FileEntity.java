package entity;

import java.util.Collections;
import java.util.UUID;
import java.util.List;

public class FileEntity {
  private UUID id;
  private String name;
  private long size;
  private String path;
  private String url;
  private List<String> tags;

  public FileEntity(UUID id, String name, long size, String path, String url, List<String> tags) {
    this.id = id;
    this.name = name;
    this.size = size;
    this.path = path;
    this.url = url;
    this.tags = tags;
  }

  public UUID getId() { return id; }
  public String getName() { return name; }
  public long getSize() { return size; }
  public String getUrl() { return url; }
  public List<String> getTags() { return tags; }
  public String getPath() { return path; }

  public void setId(UUID id) { this.id = id; }
  public void setName(String name) { this.name = name; }
  public void setSize(String size) { this.size = Long.parseLong(size); }
  public void setPath(String path) { this.path = path; }
  public void setUrl(String url) { this.url = url; }
  public void setTags(String tags) { this.tags = Collections.singletonList(tags); }
}

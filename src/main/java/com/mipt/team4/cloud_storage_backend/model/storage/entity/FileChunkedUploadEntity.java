package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.UploadChunkDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileChunkedUploadEntity {

  private final Map<Integer, String> eTags;
  private UUID sessionId;
  private UUID userId;
  private String s3UploadId;
  private String path;
  private List<String> tags;
  private List<UploadChunkDto> chunks;

  public FileChunkedUploadEntity(
      UUID sessionId,
      UUID userId,
      String s3UploadId,
      String path,
      List<String> tags,
      List<UploadChunkDto> chunks,
      Map<Integer, String> eTags) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.s3UploadId = s3UploadId;
    this.path = path;
    this.tags = tags;
    this.chunks = chunks;
    this.eTags = eTags;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public void setSessionId(UUID sessionId) {
    this.sessionId = sessionId;
  }

  public UUID getuserId() {
    return userId;
  }

  public void setuserId(UUID userId) {
    this.userId = userId;
  }

  public String getS3UploadId() {
    return s3UploadId;
  }

  public void setS3UploadId(String s3UploadId) {
    this.s3UploadId = s3UploadId;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public List<UploadChunkDto> getChunks() {
    return chunks;
  }

  public void setChunks(List<UploadChunkDto> chunks) {
    this.chunks = chunks;
  }

  public Map<Integer, String> getETags() {
    return eTags;
  }

  public String getETag(int partIndex) {
    return eTags.get(partIndex);
  }

  public void putETag(int partIndex, String eTag) {
    this.eTags.put(partIndex, eTag);
  }
}

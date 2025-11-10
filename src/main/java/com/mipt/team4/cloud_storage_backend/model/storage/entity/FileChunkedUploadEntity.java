package com.mipt.team4.cloud_storage_backend.model.storage.entity;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileChunkedUploadEntity {
  private UUID sessionId;
  private UUID ownerId;
  private long totalFileSize;
  private int totalChunks;
  private String s3UploadId;
  private String path;
  private List<String> tags;
  private List<FileChunkDto> chunks;
  private Map<Integer, String> eTags;

  public FileChunkedUploadEntity(
      UUID sessionId,
      UUID ownerId,
      long totalFileSize,
      int totalChunks,
      String s3UploadId,
      String path,
      List<String> tags,
      List<FileChunkDto> chunks,
      Map<Integer, String> eTags) {
    this.sessionId = sessionId;
    this.ownerId = ownerId;
    this.s3UploadId = s3UploadId;
    this.totalFileSize = totalFileSize;
    this.totalChunks = totalChunks;
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

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public String getS3UploadId() {
    return s3UploadId;
  }

  public void setS3UploadId(String s3UploadId) {
    this.s3UploadId = s3UploadId;
  }

  public long getTotalFileSize() {
    return totalFileSize;
  }

  public void setTotalFileSize(long totalFileSize) {
    this.totalFileSize = totalFileSize;
  }

  public int getTotalChunks() {
    return totalChunks;
  }

  public void setTotalChunks(int totalChunks) {
    this.totalChunks = totalChunks;
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

  public List<FileChunkDto> getChunks() {
    return chunks;
  }

  public void setChunks(List<FileChunkDto> chunks) {
    this.chunks = chunks;
  }

  public Map<Integer, String> getETags() {
    return eTags;
  }

  public String getETag(int partIndex) {
    return eTags.get(partIndex);
  }
  
  public void addETag(int partIndex, String eTag) {
    this.eTags.put(partIndex, eTag);
  }
}

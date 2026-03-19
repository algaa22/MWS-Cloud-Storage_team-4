package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ChunkedUploadSession {

  private final List<byte[]> chunks = new ArrayList<>();
  private final Map<Integer, String> eTags = new ConcurrentHashMap<>();
  private final StorageEntity entity;
  @Setter private ChunkedUploadState status;
  private final String uploadId;
  private long fileSize = 0;
  private int totalParts = 0;
  private int partSize = 0;
  private int partNum = 0;

  ChunkedUploadSession(String uploadId, StorageEntity entity) {
    this.entity = entity;
    this.uploadId = uploadId;
  }

  public void increaseTotalParts() {
    totalParts++;
  }

  public void addFileSize(long amount) {
    fileSize += amount;
  }

  public void addPartSize(int amount) {
    partSize += amount;
  }

  public void increasePartNum() {
    partNum++;
  }

  public void resetPartSize() {
    partSize = 0;
  }

  public void clear() {
    chunks.clear();
    eTags.clear();
  }
}

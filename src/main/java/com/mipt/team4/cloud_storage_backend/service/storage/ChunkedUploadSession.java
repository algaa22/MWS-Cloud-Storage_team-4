package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.StartChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

@Getter
public class ChunkedUploadSession {

  private final List<byte[]> chunks = new ArrayList<>();
  // TODO: сессия не удаляется, если completeMultipartUpload не вызван
  // TODO: нужны ли все поля session?
  private final StartChunkedUploadRequest request;
  private final Map<Integer, String> eTags = new ConcurrentHashMap<>();
  private final StorageEntity entity;
  private boolean stopped = false;
  private long fileSize = 0;
  private int totalParts = 0;
  private int partSize = 0;
  private int partNum = 0;
  private String uploadId;

  ChunkedUploadSession(StartChunkedUploadRequest request, String uploadId, StorageEntity entity) {
    this.request = request;
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

  public void stop() {
    stopped = true;
  }

  public void resume() {
    stopped = false;
  }

  public void clear() {
    chunks.clear();
    eTags.clear();
  }
}

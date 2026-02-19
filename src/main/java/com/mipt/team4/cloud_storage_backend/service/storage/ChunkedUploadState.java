package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.monitor.FileEntry;

public class ChunkedUploadState {

  public final List<byte[]> chunks = new ArrayList<>();
  // TODO: сессия не удаляется, если completeMultipartUpload не вызван
  // TODO: нужны ли все поля session?
  private final FileChunkedUploadRequest session;
  private final Map<Integer, String> eTags = new HashMap<>();
  private final StorageEntity entity;
  private long fileSize = 0;
  private int totalParts = 0;
  private int partSize = 0;
  private int partNum = 0;
  private String uploadId;

  ChunkedUploadState(FileChunkedUploadRequest session, StorageEntity entity) {
    this.session = session;
    this.entity = entity;
  }

  String getOrCreateUploadId(StorageRepository repo) {
    if (uploadId == null) {
      uploadId = repo.startMultipartUpload(entity);
    }

    return uploadId;
  }

  // TODO: LOMBOK
  public int getTotalParts() {
    return totalParts;
  }

  public long getFileSize() {
    return fileSize;
  }

  public String getUploadId() {
    return uploadId;
  }

  public Map<Integer, String> getETags() {
    return eTags;
  }

  public void addCompletedPart(int partIndex, String eTag) {
    eTags.put(partIndex, eTag);
  }

  public FileChunkedUploadRequest getSession() {
    return session;
  }

  public int getPartNum() {
    return partNum;
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

  public long getPartSize() {
    return partSize;
  }

  public void increasePartNum() {
    partNum++;
  }

  public void resetPartSize() {
    partSize = 0;
  }

  public StorageEntity getEntity() {
    return entity;
  }
}

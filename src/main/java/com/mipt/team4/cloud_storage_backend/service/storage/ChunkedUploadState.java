package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.FileChunkedUploadDto;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChunkedUploadState {
  // TODO: сессия не удаляется, если completeMultipartUpload не вызван
  private final UUID userId;
  private final UUID fileId;
  private final String path;
  private final FileChunkedUploadDto session;

  private long fileSize = 0;
  private int totalParts = 0;
  private int partSize = 0;
  private int partNum = 0;

  private final Map<Integer, String> eTags = new HashMap<>();
  public final List<byte[]> chunks = new ArrayList<>();
  private String uploadId;

  ChunkedUploadState(FileChunkedUploadDto session, UUID userId, UUID fileId, String path) {
    this.session = session;
    this.userId = userId;
    this.fileId = fileId;
    this.path = path;
  }

  String getOrCreateUploadId(StorageRepository repo) {
    if (uploadId == null) {
      uploadId = repo.startMultipartUpload(userId, fileId);
    }

    return uploadId;
  }

  public int getTotalParts() {
    return totalParts;
  }

  public long getFileSize() {
    return fileSize;
  }

  public String getUploadId() {
    return uploadId;
  }

  public String getPath() {
    return path;
  }

  public UUID getFileId() {
    return fileId;
  }

  public UUID getUserId() {
    return userId;
  }

  public Map<Integer, String> getETags() {
    return eTags;
  }

  public void addCompletedPart(int partIndex, String eTag) {
    eTags.put(partIndex, eTag);
  }

  public FileChunkedUploadDto getSession() {
    return session;
  }

  public int getPartNum() {
    return partNum;
  }

  public void setPartNum(int partNum) {
    this.partNum = partNum;
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
}

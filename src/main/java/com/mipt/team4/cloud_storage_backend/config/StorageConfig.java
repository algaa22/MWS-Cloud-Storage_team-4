package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;

public enum StorageConfig {
  INSTANCE;

  private final long maxFileSize;
  private final int maxFileChunkSize;
  private final int maxContentLength;
  private final int fileDownloadChunkSize;
  private final int sendUploadProgressInterval;

  StorageConfig() {
    ConfigSource source = new EnvironmentConfigSource();

    this.maxFileSize = source.getLong("storage.http.max-file-size").orElseThrow();
    this.maxFileChunkSize = source.getInt("storage.http.max-file-chunk-size").orElseThrow();
    this.maxContentLength = source.getInt("storage.http.max-content-length").orElseThrow();
    this.fileDownloadChunkSize =
        source.getInt("storage.http.file-download-chunk-size").orElseThrow();
    this.sendUploadProgressInterval =
        source.getInt("storage.http.send-upload-progress-interval").orElseThrow();
  }

  public long getMaxFileSize() {
    return maxFileSize;
  }

  public int getMaxFileChunkSize() {
    return maxFileChunkSize;
  }

  public int getMaxContentLength() {
    return maxContentLength;
  }

  public int getFileDownloadChunkSize() {
    return fileDownloadChunkSize;
  }

  public int getSendUploadProgressInterval() {
    return sendUploadProgressInterval;
  }
}

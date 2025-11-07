package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;

public class StorageConfig {
  private static volatile StorageConfig instance;
  private final long maxFileSize;
  private final int maxFileChunkSize;
  private final int maxContentLength;

  private final int fileDownloadChunkSize;

  public StorageConfig(long maxFileSize, int maxFileChunkSize, int maxContentLength, int fileDownloadChunkSize) {
    this.maxFileSize = maxFileSize;
    this.maxFileChunkSize = maxFileChunkSize;
    this.maxContentLength = maxContentLength;
    this.fileDownloadChunkSize = fileDownloadChunkSize;
  }

  public static StorageConfig getInstance() {
    if (instance == null) {
      synchronized (DatabaseConfig.class) {
        if (instance == null) {
          ConfigSource source = new EnvironmentConfigSource();

          instance = new StorageConfig(
                  source.getLong("storage.http.max-file-size").orElseThrow(),
                  source.getInt("storage.http.max-file-chunk-size").orElseThrow(),
                  source.getInt("storage.http.file-download-chunk-size").orElseThrow(),
                  source.getInt("storage.http.max-content-length").orElseThrow()
          );
        }
      }
    }

    return instance;
  }

  public long getMaxFileSize() {
    return maxFileSize;
  }

  public long getMaxFileChunkSize() {
    return maxFileChunkSize;
  }

  public int getFileDownloadChunkSize() {
    return fileDownloadChunkSize;
  }

  public int getMaxContentLength() {
    return maxContentLength;
  }
}

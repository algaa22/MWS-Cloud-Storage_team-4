package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;

public class StorageConfig {
  private final long maxFileSize;
  private final long maxFileChunkSize;

  public StorageConfig(long maxFileSize, long maxFileChunkSize) {
    this.maxFileSize = maxFileSize;
    this.maxFileChunkSize = maxFileChunkSize;
  }

  public static StorageConfig from(ConfigSource source) {
    return new StorageConfig(
            source.getLong("storage.controller.max-file-size").orElseThrow(),
            source.getLong("storage.controller.max-file-chunk-size").orElseThrow()
    );
  }

  public long getMaxFileSize() {
    return maxFileSize;
  }

  public long getMaxFileChunkSize() {
    return maxFileChunkSize;
  }
}

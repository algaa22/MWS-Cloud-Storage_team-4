package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;

public class StorageConfig {
  private static volatile StorageConfig instance;
  private final long maxFileSize;
  private final long maxFileChunkSize;

  public StorageConfig(long maxFileSize, long maxFileChunkSize) {
    this.maxFileSize = maxFileSize;
    this.maxFileChunkSize = maxFileChunkSize;
  }

  public static StorageConfig getInstance() {
    if (instance == null) {
      synchronized (DatabaseConfig.class) {
        if (instance == null) {
          ConfigSource source = new EnvironmentConfigSource();

          instance = new StorageConfig(
                  source.getLong("storage.controller.max-file-size").orElseThrow(),
                  source.getLong("storage.controller.max-file-chunk-size").orElseThrow()
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
}

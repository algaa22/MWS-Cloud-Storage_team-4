package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;

public class StorageConfig {
  private final long maxFileSize;
  private final long maxFileChunkSize;
  private final String userDataBucketName;

  public StorageConfig(long maxFileSize, long maxFileChunkSize, String userDataBucketName) {
    this.maxFileSize = maxFileSize;
    this.maxFileChunkSize = maxFileChunkSize;
    this.userDataBucketName = userDataBucketName;
  }

  public static StorageConfig from(ConfigSource source) {
    return new StorageConfig(
        source.getLong("storage.controller.max-file-size").orElseThrow(),
        source.getLong("storage.controller.max-file-chunk-size").orElseThrow(),
        source.getString("storage.repository.user-data-bucket.name").orElseThrow());
  }

  public long getMaxFileSize() {
    return maxFileSize;
  }

  public long getMaxFileChunkSize() {
    return maxFileChunkSize;
  }

  public String getUserDataBucketName() {
    return userDataBucketName;
  }
}

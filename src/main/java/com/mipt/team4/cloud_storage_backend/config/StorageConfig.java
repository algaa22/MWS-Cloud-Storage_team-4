package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;

public enum StorageConfig {
  INSTANCE;

  private final String jwtSecretKey;
  private final int maxContentLength;
  private final int jwtTokenExpirationSec;
  private final long fileDownloadChunkSize;
  private final long defaultStorageLimit;

  StorageConfig() {
    ConfigSource yamlSource = new YamlConfigSource("config.yml");
    ConfigSource envSource = new EnvironmentConfigSource(".env");

    this.maxContentLength = yamlSource.getInt("storage.http.max-content-length").orElseThrow();
    this.fileDownloadChunkSize =
        yamlSource.getLong("storage.http.file-download-chunk-size").orElseThrow();
    this.defaultStorageLimit =
        yamlSource.getLong("storage.quotas.default-storage-limit").orElseThrow();
    this.jwtTokenExpirationSec =
        yamlSource.getInt("storage.auth.jwt-token-expiration-sec").orElseThrow();

    this.jwtSecretKey = envSource.getString("jwt.secret.key").orElseThrow();
  }

  public long getDefaultStorageLimit() {
    return defaultStorageLimit;
  }

  public String getJwtSecretKey() {
    return jwtSecretKey;
  }

  public int getJwtTokenExpirationSec() {
    return jwtTokenExpirationSec;
  }

  public int getMaxContentLength() {
    return maxContentLength;
  }

  public long getFileDownloadChunkSize() {
    return fileDownloadChunkSize;
  }
}

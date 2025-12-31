package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;

public enum StorageConfig {
  INSTANCE;

  private final String jwtSecretKey;
  private final int maxAggregatedContentLength;
  private final int fileDownloadChunkSize;
  private final long defaultStorageLimit;
  private final long accessTokenExpirationSec;
  private final long refreshTokenExpirationSec;
  private final long maxFileChunkSize;
  private final long minFilePartSize;

  StorageConfig() {
    // TODO: зачем каждый раз создавать Source
    ConfigSource yamlSource = new YamlConfigSource("config.yml");
    ConfigSource envSource = new EnvironmentConfigSource(".env");

    this.maxAggregatedContentLength =
        yamlSource.getInt("storage.http.max-aggregated-content-length").orElseThrow();
    this.fileDownloadChunkSize =
        yamlSource.getInt("storage.http.file-download-chunk-size").orElseThrow();
    this.maxFileChunkSize = yamlSource.getLong("storage.http.max-file-chunk-size").orElseThrow();
    this.minFilePartSize =
        yamlSource.getLong("storage.repository.minio.min-file-part-size").orElseThrow();
    this.defaultStorageLimit =
        yamlSource.getLong("storage.quotas.default-storage-limit").orElseThrow();
    this.accessTokenExpirationSec =
        yamlSource.getInt("storage.auth.access-token-expiration-sec").orElseThrow();
    this.refreshTokenExpirationSec =
        yamlSource.getInt("storage.auth.refresh-token-expiration-sec").orElseThrow();

    this.jwtSecretKey = envSource.getString("jwt.secret.key").orElseThrow();
  }

  public String getJwtSecretKey() {
    return jwtSecretKey;
  }

  public int getMaxAggregatedContentLength() {
    return maxAggregatedContentLength;
  }

  public int getFileDownloadChunkSize() {
    return fileDownloadChunkSize;
  }

  public long getAccessTokenExpirationSec() {
    return accessTokenExpirationSec;
  }

  public long getRefreshTokenExpirationSec() {
    return refreshTokenExpirationSec;
  }

  public long getDefaultStorageLimit() {
    return defaultStorageLimit;
  }

  public long getMaxFileChunkSize() {
    return maxFileChunkSize;
  }

  public long getMinFilePartSize() {
    return minFilePartSize;
  }
}

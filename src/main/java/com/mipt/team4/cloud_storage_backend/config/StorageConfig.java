package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.factories.EnvConfigFactory;
import com.mipt.team4.cloud_storage_backend.config.sources.factories.YamlConfigFactory;

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
    ConfigSource yamlSource = YamlConfigFactory.INSTANCE.getDefault();
    ConfigSource envSource = EnvConfigFactory.INSTANCE.getDefault();

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

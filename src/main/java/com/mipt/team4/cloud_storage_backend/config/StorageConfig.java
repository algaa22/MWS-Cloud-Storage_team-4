package com.mipt.team4.cloud_storage_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageConfig(Rest rest, Auth auth, Quotas quotas) {
  public record Rest(
      int maxAggregatedContentLength,
      long maxFileSize,
      long maxFileChunkSize,
      int fileDownloadChunkSize) {}

  public record Auth(
      String jwtSecretKey, long accessTokenExpirationSec, long refreshTokenExpirationSec) {}

  public record Quotas(long defaultStorageLimit) {}
}

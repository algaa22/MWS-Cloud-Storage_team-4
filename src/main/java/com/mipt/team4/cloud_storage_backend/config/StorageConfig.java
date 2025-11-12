package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;

public enum StorageConfig {
  INSTANCE;

  private final String jwtSecretKey;
  private final long maxFileSize;
  private final int maxFileChunkSize;
  private final int maxContentLength;
  private final int fileDownloadChunkSize;
  private final int sendUploadProgressInterval;
  private final int jwtTokenExpirationMs;
  private final String userDataBucketName;

  StorageConfig() {
    ConfigSource yamlSource = new YamlConfigSource("config.yml");
    ConfigSource envSource = new EnvironmentConfigSource();

    this.maxFileSize = yamlSource.getLong("storage.http.max-file-size").orElseThrow();
    this.maxFileChunkSize = yamlSource.getInt("storage.http.max-file-chunk-size").orElseThrow();
    this.maxContentLength = yamlSource.getInt("storage.http.max-content-length").orElseThrow();
    this.fileDownloadChunkSize =
        yamlSource.getInt("storage.http.file-download-chunk-size").orElseThrow();
    this.sendUploadProgressInterval =
        yamlSource.getInt("storage.http.send-upload-progress-interval").orElseThrow();
    this.userDataBucketName =
        yamlSource.getString("storage.repository.user-data-bucket.name").orElseThrow();
    this.jwtTokenExpirationMs =
        yamlSource.getInt("storage.jwt.jwt-token-expiration-ms").orElseThrow();

    this.jwtSecretKey =
        envSource.getString("jwt.secret.key").orElseThrow(); // TODO: оставить здесь?
  }

  public String getJwtSecretKey() {
    return jwtSecretKey;
  }

  public long getMaxFileSize() {
    return maxFileSize;
  }

  public int getJwtTokenExpirationMs() {
    return jwtTokenExpirationMs;
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

  public String getUserDataBucketName() {
    return userDataBucketName;
  }
}

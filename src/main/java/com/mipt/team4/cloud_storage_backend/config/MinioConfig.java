package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;

public enum MinioConfig {
  INSTANCE;

  private final String url;
  private final String username;
  private final String password;

  MinioConfig() {
    ConfigSource source = new EnvironmentConfigSource(".env");

    this.url = source.getString("minio.url").orElseThrow();
    this.username = source.getString("minio.username").orElseThrow();
    this.password = source.getString("minio.password").orElseThrow();
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}

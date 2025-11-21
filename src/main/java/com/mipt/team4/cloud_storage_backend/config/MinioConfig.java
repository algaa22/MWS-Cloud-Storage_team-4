package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;

public enum MinioConfig {
  INSTANCE;

  private final String url;
  private final String username;
  private final String password;
  private final String userDataBucketName;

  MinioConfig() {
    ConfigSource envSource = new EnvironmentConfigSource();
    YamlConfigSource yamlSource = new YamlConfigSource("config.yml");

    this.url = envSource.getString("minio.url").orElseThrow();
    this.username = envSource.getString("minio.username").orElseThrow();
    this.password = envSource.getString("minio.password").orElseThrow();

    this.userDataBucketName =
        yamlSource.getString("storage.repository.user-data-bucket.name").orElseThrow();
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

  public String getUserDataBucketName() {
    return userDataBucketName;
  }
}

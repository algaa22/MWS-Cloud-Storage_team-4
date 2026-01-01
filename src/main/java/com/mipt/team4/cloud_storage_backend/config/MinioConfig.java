package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.factories.EnvConfigFactory;
import com.mipt.team4.cloud_storage_backend.config.sources.factories.YamlConfigFactory;

public enum MinioConfig {
  INSTANCE;

  private final String url;
  private final String username;
  private final String password;
  private final String userDataBucketName;

  MinioConfig() {
    ConfigSource envSource = EnvConfigFactory.INSTANCE.getDefault();
    YamlConfigSource yamlSource = YamlConfigFactory.INSTANCE.getDefault();

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

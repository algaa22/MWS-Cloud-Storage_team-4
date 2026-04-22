package com.mipt.team4.cloud_storage_backend.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public record MinioConfig(
    long minFilePartSize,
    String url,
    String externalUrl,
    String username,
    String password,
    String region,
    UserDataBucket userDataBucket) {

  public MinioConfig {
    if (externalUrl == null || externalUrl.isBlank()) {
      externalUrl = url;
    }
  }

  public record UserDataBucket(String name) {}
}

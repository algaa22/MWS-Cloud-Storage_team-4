package com.mipt.team4.cloud_storage_backend.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public record MinioConfig(
    long minFilePartSize,
    String url,
    String username,
    String password,
    UserDataBucket userDataBucket
) {
  public record UserDataBucket(String name) {

  }
}

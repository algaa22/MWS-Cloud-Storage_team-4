package com.mipt.team4.cloud_storage_backend.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3")
public record S3Config(
    String url,
    String username,
    String password,
    String region,
    UserDataBucket userDataBucket,
    Limits limits) {
  public record Limits(long minFilePartSize, long maxFilePartSize, int maxPartsNum) {}

  public record UserDataBucket(String name) {}
}

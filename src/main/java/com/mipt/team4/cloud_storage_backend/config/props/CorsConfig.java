package com.mipt.team4.cloud_storage_backend.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cors")
public record CorsConfig(AccessControl accessControl) {
  public record AccessControl(String allowOrigin, boolean allowCredentials, int maxAge) {}
}

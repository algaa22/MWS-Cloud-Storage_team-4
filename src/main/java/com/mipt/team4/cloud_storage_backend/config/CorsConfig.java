package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.factories.YamlConfigFactory;

public enum CorsConfig {
  INSTANCE;

  private final int maxAge;
  private final String allowOrigin;
  private final boolean allowCredentials;

  CorsConfig() {
    ConfigSource yamlSource = YamlConfigFactory.INSTANCE.getDefault();

    this.maxAge = yamlSource.getInt("cors.access-control.max-age").orElseThrow();
    this.allowOrigin = yamlSource.getString("cors.access-control.allow-origin").orElseThrow();
    this.allowCredentials = yamlSource.getBoolean("cors.access-control.allow-credentials")
        .orElseThrow();
  }

  public int getMaxAge() {
    return maxAge;
  }

  public String getAllowOrigin() {
    return allowOrigin;
  }

  public boolean isAllowCredentials() {
    return allowCredentials;
  }
}

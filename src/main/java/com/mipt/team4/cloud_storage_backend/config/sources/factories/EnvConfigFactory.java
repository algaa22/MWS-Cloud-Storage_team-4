package com.mipt.team4.cloud_storage_backend.config.sources.factories;

import com.mipt.team4.cloud_storage_backend.config.sources.EnvConfigSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum EnvConfigFactory {
  INSTANCE;

  private final Map<String, EnvConfigSource> configs = new ConcurrentHashMap<>();

  public EnvConfigSource getDefault() {
    return get(".env");
  }

  public EnvConfigSource get(String path) {
    return configs.computeIfAbsent(path, EnvConfigSource::new);
  }
}

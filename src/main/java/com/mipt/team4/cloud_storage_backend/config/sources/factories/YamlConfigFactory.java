package com.mipt.team4.cloud_storage_backend.config.sources.factories;

import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum YamlConfigFactory {
  INSTANCE;

  private final Map<String, YamlConfigSource> configs = new ConcurrentHashMap<>();

  public YamlConfigSource getDefault() {
    return get("config.yml");
  }

  public YamlConfigSource get(String path) {
    return configs.computeIfAbsent(path, YamlConfigSource::new);
  }
}

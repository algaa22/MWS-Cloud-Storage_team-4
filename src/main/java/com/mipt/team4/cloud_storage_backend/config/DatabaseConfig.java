package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;

public enum DatabaseConfig {
  INSTANCE;

  private final String url;
  private final String name;
  private final String username;
  private final String password;

  DatabaseConfig() {
    // TODO: не находит .env
    ConfigSource source = new EnvironmentConfigSource();

    this.url = source.getString("db.url").orElseThrow();
    this.name = source.getString("db.name").orElseThrow();
    this.username = source.getString("db.username").orElseThrow();
    this.password = source.getString("db.password").orElseThrow();
  }

  public String getUrl() {
    return url;
  }

  public String getName() {
    return name;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}

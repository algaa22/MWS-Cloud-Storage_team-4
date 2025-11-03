package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;

public class DatabaseConfig {
  private final String url;
  private final String username;
  private final String password;

  public DatabaseConfig(String url, String username, String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public static DatabaseConfig from(ConfigSource source) {
    return new DatabaseConfig(
            source.getString("db.url").orElseThrow(),
            source.getString("db.username").orElseThrow(),
            source.getString("db.password").orElseThrow()
    );
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
}

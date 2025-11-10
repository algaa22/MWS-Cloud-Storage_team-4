package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.ConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;

public class DatabaseConfig {
  private static volatile DatabaseConfig instance;

  private final String url;
  private final String username;
  private final String password;

  private DatabaseConfig(String url, String username, String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public static DatabaseConfig getInstance() {
    if (instance == null) {
      synchronized (DatabaseConfig.class) {
        if (instance == null) {
          ConfigSource source = new EnvironmentConfigSource();

          instance = new DatabaseConfig(
                  source.getString("db.url").orElseThrow(),
                  source.getString("db.username").orElseThrow(),
                  source.getString("db.password").orElseThrow()
          );
        }
      }
    }
    return instance;
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

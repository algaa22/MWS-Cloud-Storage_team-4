package com.mipt.team4.cloud_storage_backend.config.sources;

import java.io.*;
import java.util.Optional;

public class EnvironmentConfigSource extends ConfigSource {
  @Override
  public Optional<String> getString(String key) {
    return Optional.ofNullable(System.getenv(convertToEnvVarName(key)));
  }

  private String convertToEnvVarName(String path) {
    return path.toUpperCase().replace('.', '_').replace('-', '_');
  }
}

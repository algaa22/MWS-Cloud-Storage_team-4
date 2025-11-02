package com.mipt.team4.cloud_storage_backend.exception.config;

import java.nio.file.Path;

public class DotEnvNotFoundException extends RuntimeException {
  public DotEnvNotFoundException(Path path) {
    super(".env not found at " + path);
  }
}


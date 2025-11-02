package com.mipt.team4.cloud_storage_backend.exception.config;

import java.nio.file.Path;

public class DotEnvLoadException extends RuntimeException {
  public DotEnvLoadException(Path path, Throwable cause) {
    super("Failed to load a file with path " + path.toAbsolutePath(), cause);
  }
}

package com.mipt.team4.cloud_storage_backend.exception.config;

import java.io.IOException;

public class DotEnvLoadException extends RuntimeException {
  public DotEnvLoadException(String filePath, IOException cause) {
    super(".env file not found: path=" + filePath, cause);
  }
}

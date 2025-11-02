package com.mipt.team4.cloud_storage_backend.exception.config;

public class YamlLoadException extends RuntimeException {
  public YamlLoadException(String filePath, Throwable cause) {
    super("Failed to load YAML from " + filePath, cause);
  }
}


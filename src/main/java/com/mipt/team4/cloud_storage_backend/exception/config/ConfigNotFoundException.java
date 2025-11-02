package com.mipt.team4.cloud_storage_backend.exception.config;

public class ConfigNotFoundException extends RuntimeException {
  public ConfigNotFoundException(String filePath) {
    super("Config file " + filePath + " not found in filesystem or classpath");
  }
}

package com.mipt.team4.cloud_storage_backend.exception.config;

public class ConfigConvertException extends RuntimeException {
  public ConfigConvertException(String expectedType, String key, String value) {
    super("Cannot convert value " + value + " to " + expectedType + " for key " + key);
  }
}

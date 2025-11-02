package com.mipt.team4.cloud_storage_backend.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLOutput;
import java.util.Optional;

public abstract class ConfigSource {
  public abstract Optional<String> getString(String key);

  public String getString(String key, String defaultValue) {
    return getString(key).orElse(defaultValue);
  }

  public int getInt(String key, int defaultValue) {
    return getInt(key).orElse(defaultValue);
  }

  public float getFloat(String key, float defaultValue) {
    return getFloat(key).orElse(defaultValue);
  }

  public double getDouble(String key, double defaultValue) {
    return getDouble(key).orElse(defaultValue);
  }

  public long getLong(String key, long defaultValue) {
    return getLong(key).orElse(defaultValue);
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    return getBoolean(key).orElse(defaultValue);
  }

  public Optional<Integer> getInt(String key) {
    return getString(key)
        .flatMap(
            value -> {
              try {
                return Optional.of(Integer.parseInt(value));
              } catch (NumberFormatException e) {
                printConvertError("Integer", key, value);
                return Optional.empty();
              }
            });
  }

  public Optional<Float> getFloat(String key) {
    return getString(key)
        .flatMap(
            value -> {
              try {
                return Optional.of(Float.parseFloat(value));
              } catch (NumberFormatException e) {
                printConvertError("Float", key, value);
                return Optional.empty();
              }
            });
  }

  public Optional<Double> getDouble(String key) {
    return getString(key)
        .flatMap(
            value -> {
              try {
                return Optional.of(Double.parseDouble(value));
              } catch (NumberFormatException e) {
                printConvertError("Double", key, value);
                return Optional.empty();
              }
            });
  }

  public Optional<Long> getLong(String key) {
    return getString(key)
        .flatMap(
            value -> {
              try {
                return Optional.of(Long.parseLong(value));
              } catch (NumberFormatException e) {
                printConvertError("Long", key, value);
                return Optional.empty();
              }
            });
  }

  public Optional<Boolean> getBoolean(String key) {
    return getString(key)
        .flatMap(
            value -> {
              try {
                return Optional.of(Boolean.parseBoolean(value));
              } catch (NumberFormatException e) {
                printConvertError("Double", key, value);
                return Optional.empty();
              }
            });
  }

  protected void printConvertError(String expectedType, String key, String value) {
    System.err.println("Cannot convert value " + value +  " to " + expectedType + " for key " + key);
  }

  protected InputStream getInputStream(String filePath) {
    try {
      return new FileInputStream(filePath);
    } catch (FileNotFoundException _) {}

    InputStream classPathStream = getClass().getClassLoader().getResourceAsStream(filePath);
    if (classPathStream != null) return classPathStream;

    System.err.println("Input stream of file " + filePath + " not found");
    
    return null;
  }
}

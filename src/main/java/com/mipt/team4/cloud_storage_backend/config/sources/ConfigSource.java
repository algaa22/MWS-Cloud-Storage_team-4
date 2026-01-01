package com.mipt.team4.cloud_storage_backend.config.sources;

import com.mipt.team4.cloud_storage_backend.exception.config.ConfigConvertException;
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
                throw new ConfigConvertException("Integer", key, value);
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
                throw new ConfigConvertException("Float", key, value);
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
                throw new ConfigConvertException("Double", key, value);
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
                throw new ConfigConvertException("Long", key, value);
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
                throw new ConfigConvertException("Double", key, value);
              }
            });
  }
}

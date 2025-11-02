package com.mipt.team4.cloud_storage_backend.config;

import java.util.Optional;

public interface ConfigSource {
  Optional<String> getString(String key);

  default String getString(String key, String defaultValue) {
    return getString(key).orElse(defaultValue);
  }

  default int getInt(String key, int defaultValue) {
    return getInt(key).orElse(defaultValue);
  }

  default float getFloat(String key, float defaultValue) {
    return getFloat(key).orElse(defaultValue);
  }

  default double getDouble(String key, double defaultValue) {
    return getDouble(key).orElse(defaultValue);
  }

  default long getLong(String key, long defaultValue) {
    return getLong(key).orElse(defaultValue);
  }

  default boolean getBoolean(String key, boolean defaultValue) {
    return getBoolean(key).orElse(defaultValue);
  }

  default Optional<Integer> getInt(String key) {
    return getString(key).flatMap(value -> {
      try {
        return Optional.of(Integer.parseInt(value));
      } catch (NumberFormatException e) {
        return Optional.empty();
      }
    });
  }

  default Optional<Float> getFloat(String key) {
    return getString(key).flatMap(value -> {
      try {
        return Optional.of(Float.parseFloat(value));
      } catch (NumberFormatException e) {
        return Optional.empty();
      }
    });
  }

  default Optional<Double> getDouble(String key) {
    return getString(key).flatMap(value -> {
      try {
        return Optional.of(Double.parseDouble(value));
      } catch (NumberFormatException e) {
        return Optional.empty();
      }
    });
  }

  default Optional<Long> getLong(String key) {
    return getString(key).flatMap(value -> {
      try {
        return Optional.of(Long.parseLong(value));
      } catch (NumberFormatException e) {
        return Optional.empty();
      }
    });
  }

  default Optional<Boolean> getBoolean(String key) {
    return getString(key).flatMap(value -> {
      try {
        return Optional.of(Boolean.parseBoolean(value));
      } catch (NumberFormatException e) {
        return Optional.empty();
      }
    });
  }
}

package com.mipt.team4.cloud_storage_backend.config;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import javax.print.attribute.standard.MediaSize;

public class YamlConfigSource extends ConfigSource {
  Map<String, Object> configData;

  public YamlConfigSource(String filePath) {
    configData = loadYaml(filePath);
  }

  @Override
  public Optional<String> getString(String key) {
    Object value = configData.get(key);
    if (value == null) return Optional.empty();

    return Optional.of(value.toString());
  }

  @Override
  public Optional<Integer> getInt(String key) {
    Object value = configData.get(key);

    if (value instanceof Integer) {
      return Optional.of((int) value);
    } else if (value instanceof String) {
      return super.getInt(key);
    }

    printConvertError("Integer", key, value.toString());

    return Optional.empty();
  }

  @Override
  public Optional<Float> getFloat(String key) {
    Object value = configData.get(key);

    if (value instanceof Float) {
      return Optional.of((float) value);
    } else if (value instanceof String) {
      return super.getFloat(key);
    }

    printConvertError("Float", key, value.toString());

    return Optional.empty();
  }

  @Override
  public Optional<Double> getDouble(String key) {
    Object value = configData.get(key);

    if (value instanceof Double) {
      return Optional.of((double) value);
    } else if (value instanceof String) {
      return super.getDouble(key);
    }

    printConvertError("Double", key, value.toString());

    return Optional.empty();
  }

  @Override
  public Optional<Long> getLong(String key) {
    Object value = configData.get(key);

    if (value instanceof Long) {
      return Optional.of((long) value);
    } else if (value instanceof String) {
      return super.getLong(key);
    }

    printConvertError("Long", key, value.toString());

    return Optional.empty();
  }

  @Override
  public Optional<Boolean> getBoolean(String key) {
    Object value = configData.get(key);

    if (value instanceof Boolean) {
      return Optional.of((boolean) value);
    } else if (value instanceof String) {
      return super.getBoolean(key);
    }

    printConvertError("Boolean", key, value.toString());

    return Optional.empty();
  }

  public Optional<List<String>> getStringList(String key) {
    return getList(key, String.class);
  }

  public Optional<List<Integer>> getIntList(String key) {
    return getList(key, Integer.class);
  }

  public Optional<List<Float>> getFloatList(String key) {
    return getList(key, Float.class);
  }

  public Optional<List<Double>> getDoubleList(String key) {
    return getList(key, Double.class);
  }

  public Optional<List<Long>> getLongList(String key) {
    return getList(key, Long.class);
  }

  public Optional<List<Boolean>> getBooleanList(String key) {
    return getList(key, Boolean.class);
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
    Object value = getValue(key);

    if (value instanceof List) {
      List<Object> rawList = (List<Object>) value;
      List<T> typedList = new ArrayList<>();

      for (Object item : rawList) {
        if (elementType.isInstance(item)) {
          typedList.add((T) item);
        } else {
          System.err.println("Cannot convert list element " + item + " to " + elementType.getSimpleName());
          return Optional.empty();
        }
      }

      return Optional.of(typedList);
    }

    return Optional.empty();
  }

  public Optional<Object> getValue(String key) {
    return Optional.ofNullable(configData.get(key));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> loadYaml(String filePath) {
    Yaml yaml = new Yaml(new SafeConstructor(getYamlLoaderOptions()));

    try (InputStream inputStream = getInputStream(filePath)) {
      if (inputStream != null) {
        Object loaded = yaml.load(inputStream);

        if (loaded instanceof Map) {
          Map<String, Object> rootMap = (Map<String, Object>) loaded;
          Map<String, Object> flat = new HashMap<>();

          return flatten("", rootMap, flat);
        } else {
          System.err.println("YAML file " + filePath + " does not contain a map in root");
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to load YAML from " + filePath + ": " + e.getMessage());
    }

    return new HashMap<>();
  }

  private LoaderOptions getYamlLoaderOptions() {
    LoaderOptions loaderOptions = new LoaderOptions();

    loaderOptions.setCodePointLimit(10 * 1024 * 1024);
    loaderOptions.setMaxAliasesForCollections(50);
    loaderOptions.setNestingDepthLimit(50);
    loaderOptions.setAllowDuplicateKeys(false);

    return loaderOptions;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> flatten(String prefix, Map<String, Object> nested, Map<String, Object> flat) {
    for (Map.Entry<String, Object> entry : nested.entrySet()) {
      String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();

      if (entry.getValue() instanceof Map) {
        flatten(key, (Map<String, Object>) entry.getValue(), flat);
      } else {
        flat.put(key, entry.getValue());
      }
    }

    return flat;
  }
}

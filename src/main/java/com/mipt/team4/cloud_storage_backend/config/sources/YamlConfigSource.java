package com.mipt.team4.cloud_storage_backend.config.sources;

import com.mipt.team4.cloud_storage_backend.exception.config.ConfigConvertException;
import com.mipt.team4.cloud_storage_backend.exception.config.InvalidYamlException;
import com.mipt.team4.cloud_storage_backend.exception.config.YamlLoadException;
import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class YamlConfigSource extends ConfigSource {

  Map<String, Object> configData;

  public YamlConfigSource(String filePath) {
    configData = loadYaml(filePath);
  }

  @Override
  public Optional<String> getString(String key) {
    Object value = configData.get(key);
    if (value == null) {
      return Optional.empty();
    }

    return Optional.of(value.toString());
  }

  @Override
  public Optional<Integer> getInt(String key) {
    Object value = configData.get(key);

    return switch (value) {
      case null -> Optional.empty();
      case Integer i -> Optional.of((int) value);
      case String s -> super.getInt(key);
      default -> throw new ConfigConvertException("Integer", key, value.toString());
    };
  }

  @Override
  public Optional<Float> getFloat(String key) {
    Object value = configData.get(key);

    return switch (value) {
      case null -> Optional.empty();
      case Double v -> Optional.of(v.floatValue());
      case String s -> super.getFloat(key);
      default -> throw new ConfigConvertException("Float", key, value.toString());
    };
  }

  @Override
  public Optional<Double> getDouble(String key) {
    Object value = configData.get(key);

    return switch (value) {
      case null -> Optional.empty();
      case Double v -> Optional.of((double) value);
      case String s -> super.getDouble(key);
      default -> throw new ConfigConvertException("Double", key, value.toString());
    };
  }

  @Override
  public Optional<Long> getLong(String key) {
    Object value = configData.get(key);

    return switch (value) {
      case null -> Optional.empty();
      case Long l -> Optional.of((long) value);
      case Integer i -> Optional.of(i.longValue());
      case String s -> super.getLong(key);
      default -> throw new ConfigConvertException("Long", key, value.toString());
    };
  }

  @Override
  public Optional<Boolean> getBoolean(String key) {
    Object value = configData.get(key);

    return switch (value) {
      case null -> Optional.empty();
      case Boolean b -> Optional.of((boolean) value);
      case String s -> super.getBoolean(key);
      default -> throw new ConfigConvertException("Boolean", key, value.toString());
    };
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

  public Optional<List<Object>> getObjectList(String key) {
    return getList(key, Object.class);
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
    Object value = configData.get(key);

    if (value instanceof List) {
      List<Object> rawList = (List<Object>) value;
      List<T> typedList = new ArrayList<>();

      for (Object item : rawList) {
        if (elementType.isInstance(item)) {
          typedList.add((T) item);
        } else {
          throw new ConfigConvertException(elementType.getSimpleName(), key, item.toString());
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

    try (InputStream inputStream = FileLoader.getInputStream(filePath)) {
      Object loaded = yaml.load(inputStream);

      if (loaded instanceof Map) {
        Map<String, Object> rootMap = (Map<String, Object>) loaded;
        Map<String, Object> flat = new HashMap<>();

        return flatten("", rootMap, flat);
      } else {
        throw new InvalidYamlException(loaded, filePath);
      }
    } catch (IOException e) {
      throw new YamlLoadException(filePath, e);
    }
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
  private Map<String, Object> flatten(
      String prefix, Map<String, Object> nested, Map<String, Object> flat) {
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

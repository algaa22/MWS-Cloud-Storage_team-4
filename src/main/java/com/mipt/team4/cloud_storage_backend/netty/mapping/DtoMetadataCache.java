package com.mipt.team4.cloud_storage_backend.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.mapping.DtoIsNotRecordException;
import com.mipt.team4.cloud_storage_backend.exception.mapping.NoConstructorFoundException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DtoMetadataCache {
  private final Map<Class<?>, Constructor<?>> constructors = new ConcurrentHashMap<>();
  private final Map<Class<?>, MappedParameter[]> parameters = new ConcurrentHashMap<>();

  public Constructor<?> getConstructor(Class<?> clazz) {
    if (!constructors.containsKey(clazz)) {
      register(clazz);
    }

    return constructors.get(clazz);
  }

  public MappedParameter[] getParameters(Class<?> clazz) {
    if (!parameters.containsKey(clazz)) {
      register(clazz);
    }

    return parameters.get(clazz);
  }

  public void register(Class<?> clazz) {
    if (!clazz.isRecord()) {
      throw new DtoIsNotRecordException(clazz);
    }

    Constructor<?> constructor =
        Arrays.stream(clazz.getConstructors())
            .max(Comparator.comparingInt(Constructor::getParameterCount))
            .orElseThrow(() -> new NoConstructorFoundException(clazz));

    MappedParameter[] mappings =
        Arrays.stream(constructor.getParameters())
            .map(MappedParameter::from)
            .filter(Objects::nonNull)
            .toArray(MappedParameter[]::new);

    constructors.put(clazz, constructor);
    parameters.put(clazz, mappings);
  }
}

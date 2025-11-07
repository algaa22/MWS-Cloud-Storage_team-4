package com.mipt.team4.cloud_storage_backend.repository.config;

import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;
import com.mipt.team4.cloud_storage_backend.config.sources.YamlConfigSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YamlConfigTest {
  private static YamlConfigSource source;

  @BeforeAll
  static void beforeAll() {
    source = new YamlConfigSource("config/test.yml");
  }

  @Test
  public void shouldGetStringValue() {
    Optional<String> value = source.getString("some-string");

    assertTrue(value.isPresent());
    assertEquals("some string", value.get());
  }

  @Test
  public void shouldGetIntValue() {
    Optional<Integer> value = source.getInt("some-int");

    assertTrue(value.isPresent());
    assertEquals(123, value.get());
  }

  @Test
  public void shouldGetFloatValue() {
    Optional<Float> value = source.getFloat("some-fractional-num");

    assertTrue(value.isPresent());
    assertEquals(123.123f, value.get());
  }

  @Test
  public void shouldGetDoubleValue() {
    Optional<Double> value = source.getDouble("some-fractional-num");

    assertTrue(value.isPresent());
    assertEquals(123.123, value.get());
  }

  @Test
  public void shouldGetLongValue() {
    Optional<Long> value = source.getLong("some-long");

    assertTrue(value.isPresent());
    assertEquals(9223372036854775L, value.get());
  }

  @Test
  public void shouldGetBooleanValue() {
    Optional<Boolean> value = source.getBoolean("some-boolean");

    assertTrue(value.isPresent());
    assertEquals(true, value.get());
  }

  @Test
  public void shouldGetStringList() {
    Optional<List<String>> value = source.getStringList("some-string-list");

    assertTrue(value.isPresent());
    assertEquals(2, value.get().size());
    assertEquals("1st string", value.get().get(0));
    assertEquals("2nd string", value.get().get(1));
  }

  @Test
  public void shouldGetIntList() {
    Optional<List<Integer>> value = source.getIntList("some-int-list");

    assertTrue(value.isPresent());
    assertEquals(2, value.get().size());
    assertEquals(1, value.get().get(0));
    assertEquals(2, value.get().get(1));
  }

  @Test
  public void shouldGetValueInNestedKey() {
    Optional<String> value = source.getString("some.nested.string");

    assertTrue(value.isPresent());
    assertEquals("some nested string", value.get());
  }
}

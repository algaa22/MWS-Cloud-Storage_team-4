package com.mipt.team4.cloud_storage_backend.config;

import com.mipt.team4.cloud_storage_backend.config.sources.EnvironmentConfigSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public final class EnvironmentConfigTest {
  private static EnvironmentConfigSource source;

  @BeforeAll
  static void beforeAll() {
    source = new EnvironmentConfigSource("config/test.env");
  }

  @Test
  void shouldGetEnvVar() {
    Optional<String> value = source.getString("USER");

    assertTrue(value.isPresent());
    assertEquals(System.getenv("USER"), value.get());
  }

  @Test
  void shouldGetString() {
    Optional<String> value = source.getString("SOME_STRING");

    assertTrue(value.isPresent());
    assertEquals("some string", value.get());
  }

  @Test
  void shouldGetValue_WhenKeyHasDot() {
    Optional<String> value = source.getString("SOME.STRING");

    assertTrue(value.isPresent());
    assertEquals("some string", value.get());
  }

  @Test
  void shouldGetValue_WhenKeyIsNotInUppercase() {
    Optional<String> value = source.getString("soMe_sTring");

    assertTrue(value.isPresent());
    assertEquals("some string", value.get());
  }

  @Test
  void shouldGetValue_WhenKeyHasDash() {
    Optional<String> value = source.getString("SOME-STRING");

    assertTrue(value.isPresent());
    assertEquals("some string", value.get());
  }

  @Test
  void shouldReturnEmpty_WhenKeyDoesNotExist() {
    Optional<String> value = source.getString("askdsadasd");

    assertFalse(value.isPresent());
  }

  @Test
  void shouldGetAndConvertStringToInt() {
    Optional<Integer> value = source.getInt("SOME_INT");

    assertTrue(value.isPresent());
    assertEquals(10, value.get());
  }

  @Test
  void shouldGetAndConvertStringToFloat() {
    Optional<Float> value = source.getFloat("SOME_FRACTIONAL_NUM");

    assertTrue(value.isPresent());
    assertEquals(123.123f, value.get());
  }

  @Test
  void shouldGetAndConvertStringToDouble() {
    Optional<Double> value = source.getDouble("SOME_FRACTIONAL_NUM");

    assertTrue(value.isPresent());
    assertEquals(123.123, value.get());
  }

  @Test
  void shouldGetAndConvertIntStringToFloat() {
    Optional<Float> value = source.getFloat("SOME_INT");

    assertTrue(value.isPresent());
    assertEquals(10.0f, value.get());
  }

  @Test
  void shouldGetAndConvertStringToBoolean() {
    Optional<Boolean> value = source.getBoolean("SOME_BOOLEAN");

    assertTrue(value.isPresent());
    assertEquals(true, value.get());
  }
}

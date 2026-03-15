package com.mipt.team4.cloud_storage_backend.utils;

import com.mipt.team4.cloud_storage_backend.exception.utils.MissingRequiredParamException;
import com.mipt.team4.cloud_storage_backend.exception.utils.UnknownParamTypeException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SafeParser {
  public static Object parse(
      String value, Class<?> type, String defaultStr, boolean required, String field) {
    if (value == null || value.isBlank()) {
      if (required && (defaultStr == null || defaultStr.isBlank())) {
        throw new MissingRequiredParamException(field);
      }

      value = (defaultStr != null && !defaultStr.isBlank()) ? defaultStr : null;
    }

    if (value == null) return null;

    if (type == String.class) return value;
    if (type == UUID.class) return parseUuid(field, value);
    if (type == List.class) return parseStringList(value);
    if (type == Integer.class || type == int.class) return parseInt(field, value);
    if (type == Long.class || type == long.class) return parseLong(field, value);
    if (type == Boolean.class || type == boolean.class) return parseBoolean(field, value);
    if (type == Float.class || type == float.class) return parseFloat(field, value);
    if (type == Double.class || type == double.class) return parseDouble(field, value);

    throw new UnknownParamTypeException(field, type);
  }

  public static List<String> parseStringList(String value) {
    if (value == null || value.isBlank()) return List.of();

    return Arrays.stream(value.split(",")).map(String::trim).toList();
  }

  public static UUID parseUuid(String field, String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new ParseException(field, UUID.class, value);
    }
  }

  public static Boolean parseBoolean(String field, String value) {
    if (value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("on"))
      return true;
    if (value.equalsIgnoreCase("false") || value.equals("0") || value.equalsIgnoreCase("off"))
      return false;

    throw new ParseException(field, Boolean.class, value);
  }

  public static Integer parseInt(String field, String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new ParseException(field, Integer.class, value);
    }
  }

  public static Long parseLong(String field, String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new ParseException(field, Long.class, value);
    }
  }

  public static Float parseFloat(String field, String value) {
    try {
      return Float.parseFloat(value);
    } catch (NumberFormatException e) {
      throw new ParseException(field, Float.class, value);
    }
  }

  public static Double parseDouble(String field, String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new ParseException(field, Double.class, value);
    }
  }
}

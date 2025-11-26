package com.mipt.team4.cloud_storage_backend.utils;

import com.mipt.team4.cloud_storage_backend.exception.validation.ParseException;

public class SafeParser {
  public static Boolean parseBoolean(String field, String value) throws ParseException {
    if (value == null) return null;

    return Boolean.parseBoolean(value);
  }

  public static Integer parseInt(String field, String value) throws ParseException {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new ParseException(field, Integer.class, value);
    }
  }

  public static Long parseLong(String field, String value) throws ParseException {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new ParseException(field, Long.class, value);
    }
  }

  public static Float parseFloat(String field, String value) throws ParseException {
    try {
      return Float.parseFloat(value);
    } catch (NumberFormatException e) {
      throw new ParseException(field, Float.class, value);
    }
  }

  public static Double parseDouble(String field, String value) throws ParseException {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new ParseException(field, Double.class, value);
    }
  }
}

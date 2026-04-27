package com.mipt.team4.cloud_storage_backend.utils.converter;

import java.util.Arrays;
import java.util.List;

public class StringListConverter {
  private static final String SEPARATOR = ",";

  public static List<String> toList(String string) {
    if (string == null) {
      return null;
    }

    return Arrays.stream(string.split(SEPARATOR))
        .map(String::trim)
        .filter(trim -> !trim.isEmpty())
        .toList();
  }

  public static String toString(List<String> list) {
    if (list == null) {
      return null;
    }

    return String.join(SEPARATOR, list);
  }
}

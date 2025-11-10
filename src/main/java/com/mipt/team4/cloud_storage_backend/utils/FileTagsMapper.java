package com.mipt.team4.cloud_storage_backend.utils;

import java.util.Arrays;
import java.util.List;

public class FileTagsMapper {
  private static final String SEPARATOR = "/";

  public static List<String> toList(String tags) {
    return Arrays.stream(tags.split(SEPARATOR)).filter(tag -> !tag.trim().isEmpty()).toList();
  }

  public static String toString(List<String> tags) {
    return String.join(SEPARATOR, tags);
  }
}

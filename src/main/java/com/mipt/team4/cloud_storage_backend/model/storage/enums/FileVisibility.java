package com.mipt.team4.cloud_storage_backend.model.storage.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum FileVisibility {
  PUBLIC,
  PRIVATE,
  LINK_ONLY;

  public static final String NAMES =
      Arrays.stream(values()).map(FileVisibility::toString).collect(Collectors.joining(", "));

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}

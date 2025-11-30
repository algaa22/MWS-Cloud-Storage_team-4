package com.mipt.team4.cloud_storage_backend.model.storage.enums;

public enum FileVisibility {
  PUBLIC,
  PRIVATE,
  LINK_ONLY;

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}

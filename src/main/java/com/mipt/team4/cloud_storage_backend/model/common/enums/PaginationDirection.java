package com.mipt.team4.cloud_storage_backend.model.common.enums;

import lombok.Getter;

public enum PaginationDirection {
  ASC("asc"),
  DESC("desc");

  public static final String DEFAULT_VALUE = "desc";

  @Getter private final String name;

  PaginationDirection(String name) {
    this.name = name;
  }
}

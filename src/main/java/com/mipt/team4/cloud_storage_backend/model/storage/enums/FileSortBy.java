package com.mipt.team4.cloud_storage_backend.model.storage.enums;

import com.mipt.team4.cloud_storage_backend.model.common.enums.SortableParam;
import lombok.Getter;

public enum FileSortBy implements SortableParam {
  NAME("name"),
  DATE("updated_at"),
  SIZE("size"),
  TYPE("mime_type");

  public static final String DEFAULT_VALUE = "type";

  @Getter private final String columnName;

  FileSortBy(String columnName) {
    this.columnName = columnName;
  }
}

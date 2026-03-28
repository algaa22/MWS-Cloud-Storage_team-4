package com.mipt.team4.cloud_storage_backend.model.storage.enums;

import com.mipt.team4.cloud_storage_backend.model.common.enums.SortableParam;
import lombok.Getter;

public enum FileSortBy implements SortableParam {
  NAME("name", "name"),
  DATE("date", "updated_at"),
  SIZE("size", "size"),
  MIME_TYPE("type", "mime_type");

  @Getter private final String clientName;
  @Getter private final String columnName;

  FileSortBy(String clientName, String columnName) {
    this.clientName = clientName;
    this.columnName = columnName;
  }
}

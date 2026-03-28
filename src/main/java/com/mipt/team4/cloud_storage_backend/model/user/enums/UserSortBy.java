package com.mipt.team4.cloud_storage_backend.model.user.enums;

import com.mipt.team4.cloud_storage_backend.model.common.enums.SortableParam;
import lombok.Getter;

public enum UserSortBy implements SortableParam {
  USERNAME("username"),
  EMAIL("email"),
  CREATED_AT("created_at"),
  STORAGE_USED("used_storage"),
  TARIFF("tariff_plan");

  public static final String DEFAULT_VALUE = "username";

  @Getter private final String columnName;

  UserSortBy(String columnName) {
    this.columnName = columnName;
  }
}

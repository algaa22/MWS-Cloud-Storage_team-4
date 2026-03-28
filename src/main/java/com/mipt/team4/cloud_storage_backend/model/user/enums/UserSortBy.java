package com.mipt.team4.cloud_storage_backend.model.user.enums;

import com.mipt.team4.cloud_storage_backend.model.common.enums.SortableParam;
import lombok.Getter;

public enum UserSortBy implements SortableParam {
  USERNAME("username", "username"),
  EMAIL("email", "email"),
  CREATED_AT("date", "created_at"),
  STORAGE_USED("used_storage", "used_storage"),
  TARIFF("tariff", "tariff_plan");

  @Getter private final String clientName;
  @Getter private final String columnName;

  UserSortBy(String clientName, String columnName) {
    this.clientName = clientName;
    this.columnName = columnName;
  }
}

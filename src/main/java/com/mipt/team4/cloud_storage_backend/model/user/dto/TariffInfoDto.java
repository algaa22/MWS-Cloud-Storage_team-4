package com.mipt.team4.cloud_storage_backend.model.user.dto;

import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;

import java.time.LocalDateTime;

import lombok.Value;

@Value
public class TariffInfoDto {
  TariffPlan tariffPlan;
  long storageLimit;
  long usedStorage;
  LocalDateTime startDate;
  LocalDateTime endDate;
  boolean autoRenew;
  boolean isActive;
  int daysLeft;

  public TariffInfoDto(
      TariffPlan plan,
      long limit,
      long used,
      LocalDateTime start,
      LocalDateTime end,
      boolean autoRenew,
      boolean active) {
    this.tariffPlan = plan;
    this.storageLimit = limit;
    this.usedStorage = used;
    this.startDate = start;
    this.endDate = end;
    this.autoRenew = autoRenew;
    this.isActive = active;
    this.daysLeft =
        end != null ? (int) java.time.Duration.between(LocalDateTime.now(), end).toDays() : 0;
  }
}

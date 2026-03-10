package com.mipt.team4.cloud_storage_backend.model.user.enums;

public enum TariffPlan {
  TRIAL(10L * 1024 * 1024 * 1024, 30, 0),
  BASIC(10L * 1024 * 1024 * 1024, 30, 99),
  WORK(50L * 1024 * 1024 * 1024, 30, 199),
  PREMIUM(100L * 1024 * 1024 * 1024, 30, 349);

  private final long storageLimit;
  private final int durationDays;
  private final int priceRub;

  TariffPlan(long storageLimit, int durationDays, int priceRub) {
    this.storageLimit = storageLimit;
    this.durationDays = durationDays;
    this.priceRub = priceRub;
  }

  public long getStorageLimit() {
    return storageLimit;
  }

  public int getDurationDays() {
    return durationDays;
  }

  public int getPriceRub() {
    return priceRub;
  }

  public boolean isTrial() {
    return this == TRIAL;
  }
}

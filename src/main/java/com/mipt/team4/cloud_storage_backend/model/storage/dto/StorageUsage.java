package com.mipt.team4.cloud_storage_backend.model.storage.dto;

public record StorageUsage(long used, long limit) {
  public double getRatio() {
    return (double) used / limit;
  }
}

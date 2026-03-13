package com.mipt.team4.cloud_storage_backend.model.storage.dto;

public interface StorageUsageProjection {
  long getUsedStorage();

  long getStorageLimit();
}

package com.mipt.team4.cloud_storage_backend.model.storage.projection;

public interface StorageUsageProjection {
  long getUsedStorage();

  long getStorageLimit();
}

package com.mipt.team4.cloud_storage_backend.model.storage.dto;

public interface StorageUsageProjection {
  Long getUsedStorage(); // Long вместо long

  Long getFreeStorageLimit(); // Добавьте это поле

  Long getPaidStorageLimit(); // Добавьте это поле
}

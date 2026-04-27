package com.mipt.team4.cloud_storage_backend.utils.string;

import java.util.UUID;

public class StoragePaths {
  public static String getS3Key(UUID userId, UUID fileId) {
    return userId + "/" + fileId;
  }
}

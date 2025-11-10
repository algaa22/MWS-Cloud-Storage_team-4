package com.mipt.team4.cloud_storage_backend.utils.validation;

import java.util.UUID;

public class StoragePaths {
  public static String getS3Key(UUID ownerId, String filePath) {
    return ownerId + "/" + filePath;
  }

  public static int toS3PartIndex(int chunkIndex) {
    return chunkIndex + 1;
  }
}

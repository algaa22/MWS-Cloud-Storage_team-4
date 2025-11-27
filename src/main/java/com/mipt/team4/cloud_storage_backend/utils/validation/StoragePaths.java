package com.mipt.team4.cloud_storage_backend.utils.validation;

import java.util.StringTokenizer;
import java.util.UUID;

public class StoragePaths {
  public static String getS3Key(UUID ownerId, String filePath) {
    return ownerId + "/" + filePath;
  }

  public static String getFilePathFromS3Key(String s3Key) {
    return s3Key.substring(s3Key.indexOf("/") + 1);
  }
}

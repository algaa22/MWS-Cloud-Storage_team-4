package com.mipt.team4.cloud_storage_backend.utils;

public final class FormatUtils {

  private FormatUtils() {
  }

  public static String formatBytes(long bytes) {
    if (bytes < 0) {
      return "0 B";
    }
    if (bytes < 1024) {
      return bytes + " B";
    }

    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp - 1) + "";
    return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
  }
}
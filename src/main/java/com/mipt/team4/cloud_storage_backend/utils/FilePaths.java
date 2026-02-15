package com.mipt.team4.cloud_storage_backend.utils;

public class FilePaths {

  public static String getFileExtension(String filePath) {
    if (filePath == null) {
      return "";
    }
    int lastDot = filePath.lastIndexOf('.');

    return (lastDot > 0) ? filePath.substring(lastDot + 1).toLowerCase() : "";
  }
}

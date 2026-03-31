package com.mipt.team4.cloud_storage_backend.utils.string;

public class FileNames {

  public static String getFileExtension(String fileName) {
    if (fileName == null) {
      return "";
    }
    int lastDot = fileName.lastIndexOf('.');

    return (lastDot > 0) ? fileName.substring(lastDot + 1).toLowerCase() : "";
  }
}

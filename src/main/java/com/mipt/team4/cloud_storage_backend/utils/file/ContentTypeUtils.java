package com.mipt.team4.cloud_storage_backend.utils.file;

import java.nio.file.Files;
import java.nio.file.Path;

public class ContentTypeUtils {

  public static String detectContentType(String fileName) {
    try {
      String type = Files.probeContentType(Path.of(fileName));
      if (type != null) return type;
    } catch (Exception ignored) {
    }

    String ext = fileName.toLowerCase();

    if (ext.endsWith(".pdf")) return "application/pdf";

    if (ext.endsWith(".jpg") || ext.endsWith(".jpeg")) return "image/jpeg";
    if (ext.endsWith(".png")) return "image/png";
    if (ext.endsWith(".gif")) return "image/gif";
    if (ext.endsWith(".webp")) return "image/webp";

    if (ext.endsWith(".mp4")) return "video/mp4";
    if (ext.endsWith(".webm")) return "video/webm";
    if (ext.endsWith(".mov")) return "video/quicktime";

    if (ext.endsWith(".txt")) return "text/plain";

    return "application/octet-stream";
  }
}

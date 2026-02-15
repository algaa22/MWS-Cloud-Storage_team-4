package com.mipt.team4.cloud_storage_backend.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MimeTypeDetector {

  private static final Map<String, String> EXTENSION_MAP =
      Map.ofEntries(
          Map.entry("jpg", "image/jpeg"),
          Map.entry("jpeg", "image/jpeg"),
          Map.entry("png", "image/png"),
          Map.entry("gif", "image/gif"),
          Map.entry("bmp", "image/bmp"),
          Map.entry("webp", "image/webp"),
          Map.entry("svg", "image/svg+xml"),
          Map.entry("pdf", "application/pdf"),
          Map.entry("zip", "application/zip"),
          Map.entry("rar", "application/x-rar-compressed"),
          Map.entry("7z", "application/x-7z-compressed"),
          Map.entry("tar", "application/x-tar"),
          Map.entry("gz", "application/gzip"),
          Map.entry("doc", "application/msword"),
          Map.entry(
              "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
          Map.entry("xls", "application/vnd.ms-excel"),
          Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
          Map.entry("ppt", "application/vnd.ms-powerpoint"),
          Map.entry(
              "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
          Map.entry("txt", "text/plain"),
          Map.entry("html", "text/html"),
          Map.entry("htm", "text/html"),
          Map.entry("css", "text/css"),
          Map.entry("js", "application/javascript"),
          Map.entry("json", "application/json"),
          Map.entry("xml", "application/xml"),
          Map.entry("csv", "text/csv"),
          Map.entry("mp3", "audio/mpeg"),
          Map.entry("wav", "audio/wav"),
          Map.entry("ogg", "audio/ogg"),
          Map.entry("flac", "audio/flac"),
          Map.entry("mp4", "video/mp4"),
          Map.entry("avi", "video/x-msvideo"),
          Map.entry("mov", "video/quicktime"),
          Map.entry("mkv", "video/x-matroska"),
          Map.entry("webm", "video/webm"),
          Map.entry("java", "text/x-java-source"),
          Map.entry("py", "text/x-python"),
          Map.entry("c", "text/x-c"),
          Map.entry("cpp", "text/x-c++"),
          Map.entry("h", "text/x-c"),
          Map.entry("cs", "text/x-csharp"));

  public static String detect(String path) {
    if (path == null || path.trim().isEmpty()) {
      return "application/octet-stream";
    }

    try {
      String contentType = Files.probeContentType(Path.of(path));
      if (contentType != null && !contentType.trim().isEmpty()) {
        return contentType;
      }
    } catch (IOException e) {
    }

    return detectByExtension(path);
  }

  private static String detectByExtension(String path) {
    String extension = FilePaths.getFileExtension(path);
    return EXTENSION_MAP.getOrDefault(extension, "application/octet-stream");
  }
}

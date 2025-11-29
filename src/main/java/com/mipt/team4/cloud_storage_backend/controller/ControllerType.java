package com.mipt.team4.cloud_storage_backend.controller;

public enum ControllerType {
  FILE,
  USER;

  public static ControllerType fromUri(String uri) {
    if (uri.startsWith("/api/files") || uri.startsWith("/api/directories")) {
      return ControllerType.FILE;
    } else if (uri.startsWith("/api/auth")) {
      return USER;
    } else {
      return null;
    }
  }
}

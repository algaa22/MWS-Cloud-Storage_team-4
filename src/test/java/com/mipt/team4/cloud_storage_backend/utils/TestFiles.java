package com.mipt.team4.cloud_storage_backend.utils;

import java.io.IOException;

public enum TestFiles {
  SMALL_FILE(TestConstants.SMALL_FILE_LOCAL_PATH),
  BIG_FILE(TestConstants.BIG_FILE_LOCAL_PATH);

  private final String path;
  private byte[] data;

  TestFiles(String path) {
    this.path = path;
  }

  public byte[] getData() {
    if (data == null) {
      try {
        data = FileLoader.getInputStream(path).readAllBytes();
      } catch (IOException e) {
        throw new RuntimeException("Failed to load test file: " + path, e);
      }
    }

    return data;
  }
}

package com.mipt.team4.cloud_storage_backend.exception.utils;

public class InputStreamNotFoundException extends RuntimeException {

  public InputStreamNotFoundException(String filePath) {
    super("File " + filePath + " not found in filesystem or classpath");
  }
}

package com.mipt.team4.cloud_storage_backend.exception.config;


public class InvalidYamlException extends RuntimeException {
  public InvalidYamlException(Object loadedYaml, String filePath) {
    super(
        "Expected configuration "
            + filePath
            + " to be a map, but got: "
            + loadedYaml.getClass().getSimpleName());
  }
}

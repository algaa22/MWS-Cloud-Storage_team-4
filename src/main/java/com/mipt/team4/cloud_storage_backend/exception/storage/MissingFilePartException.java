package com.mipt.team4.cloud_storage_backend.exception.storage;

public class MissingFilePartException extends Exception {
  public MissingFilePartException(int index) {
    super("Missing file part #" + index);
  }
}

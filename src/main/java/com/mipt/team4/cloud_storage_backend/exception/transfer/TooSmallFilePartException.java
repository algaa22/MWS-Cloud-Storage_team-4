package com.mipt.team4.cloud_storage_backend.exception.transfer;

public class TooSmallFilePartException extends Exception {
  public TooSmallFilePartException() {
    super("File part must be at least 5 MB in size"); // TODO: hardcoding...
  }
}

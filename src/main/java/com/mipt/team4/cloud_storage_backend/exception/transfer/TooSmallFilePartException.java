package com.mipt.team4.cloud_storage_backend.exception.transfer;

public class TooSmallFilePartException extends Exception {

  public TooSmallFilePartException(long minFilePartSize) {
    super("File part must be at least " + minFilePartSize + " bytes in size");
  }
}

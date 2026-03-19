package com.mipt.team4.cloud_storage_backend.exception.netty;

public class MissingHostHeaderException extends RuntimeException {
  public MissingHostHeaderException() {
    super("Missing required `Host` header");
  }
}

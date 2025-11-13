package com.mipt.team4.cloud_storage_backend.exception.netty;

public class HeaderNotFoundException extends Exception {
  public HeaderNotFoundException(String headerName) {
    super("Missing required header: " + headerName);
  }
}

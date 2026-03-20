package com.mipt.team4.cloud_storage_backend.exception.share;

public class SharePasswordRequiredException extends RuntimeException {
  public SharePasswordRequiredException(String token) {
    super("Password required for share: " + token);
  }
}

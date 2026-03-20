package com.mipt.team4.cloud_storage_backend.exception.share;

public class ShareExpiredException extends RuntimeException {
  public ShareExpiredException(String token) {
    super("Share link has expired: " + token);
  }
}

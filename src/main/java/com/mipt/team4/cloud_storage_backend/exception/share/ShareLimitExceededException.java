package com.mipt.team4.cloud_storage_backend.exception.share;

public class ShareLimitExceededException extends RuntimeException {
  public ShareLimitExceededException(String token) {
    super("Download limit exceeded for share: " + token);
  }
}

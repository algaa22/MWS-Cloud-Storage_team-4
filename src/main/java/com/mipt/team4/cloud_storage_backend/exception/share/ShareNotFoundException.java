package com.mipt.team4.cloud_storage_backend.exception.share;

public class ShareNotFoundException extends RuntimeException {
  public ShareNotFoundException(String token) {
    super("Share not found with token: " + token);
  }

  public ShareNotFoundException(java.util.UUID id) {
    super("Share not found with id: " + id);
  }
}

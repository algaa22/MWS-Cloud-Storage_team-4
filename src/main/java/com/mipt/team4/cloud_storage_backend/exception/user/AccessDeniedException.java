package com.mipt.team4.cloud_storage_backend.exception.user;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class AccessDeniedException extends BaseStorageException {

  public AccessDeniedException(String message) {
    super(message, HttpResponseStatus.FORBIDDEN);
  }

  public AccessDeniedException(String message, HttpResponseStatus status) {
    super(message, status);
  }

  public static AccessDeniedException cannotUpload() {
    return new AccessDeniedException(
        "Cannot upload files: subscription expired or account restricted");
  }

  public static AccessDeniedException cannotModifyTags() {
    return new AccessDeniedException(
        "Cannot modify tags: subscription expired or account restricted");
  }

  public static AccessDeniedException cannotRenameFile() {
    return new AccessDeniedException(
        "Cannot rename file: subscription expired or account restricted");
  }
}

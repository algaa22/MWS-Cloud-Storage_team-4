package com.mipt.team4.cloud_storage_backend.exception.share;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.UUID;

public class ShareNotFoundException extends BaseStorageException {
  public ShareNotFoundException(String token) {
    super("Share not found with token: " + token, HttpResponseStatus.NOT_FOUND);
  }

  public ShareNotFoundException(UUID id) {
    super("Share not found with id: " + id, HttpResponseStatus.NOT_FOUND);
  }
}

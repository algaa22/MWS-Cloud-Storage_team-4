package com.mipt.team4.cloud_storage_backend.exception.share;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ShareFileDeletedException extends BaseStorageException {

  public ShareFileDeletedException(String message) {
    super(message, HttpResponseStatus.NOT_FOUND);
  }

  public ShareFileDeletedException(String token, String fileId) {
    super(
        String.format("File %s linked to share %s has been deleted", fileId, token),
        HttpResponseStatus.NOT_FOUND);
  }

  public ShareFileDeletedException() {
    super("The file has been deleted", HttpResponseStatus.NOT_FOUND);
  }
}

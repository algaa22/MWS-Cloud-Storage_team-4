package com.mipt.team4.cloud_storage_backend.exception.netty;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class ServerStartException extends FatalStorageException {

  public ServerStartException(Throwable cause) {
    super("Failed to start the server", cause);
  }
}

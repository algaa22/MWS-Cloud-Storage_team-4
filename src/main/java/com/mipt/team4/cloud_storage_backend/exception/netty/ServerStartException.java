package com.mipt.team4.cloud_storage_backend.exception.netty;

public class ServerStartException extends RuntimeException {
  public ServerStartException(Throwable cause) {
    super("Failed to start the server", cause);
  }
}

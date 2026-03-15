package com.mipt.team4.cloud_storage_backend.exception.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class ScanRouteException extends FatalStorageException {
  public ScanRouteException(String className, Throwable cause) {
    super("Failed to scan route for class " + className, cause);
  }
}

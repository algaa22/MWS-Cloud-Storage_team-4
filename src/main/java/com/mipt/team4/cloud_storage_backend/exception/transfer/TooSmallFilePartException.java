package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;

public class TooSmallFilePartException extends Exception {
  public TooSmallFilePartException() {
    super(
        "File part must be at least "
            + StorageConfig.INSTANCE.getMinFilePartSize()
            + " bytes in size");
  }
}

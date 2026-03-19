package com.mipt.team4.cloud_storage_backend.exception.utils;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class UnknownChecksumAlgorithmException extends FatalStorageException {
  public UnknownChecksumAlgorithmException(Throwable cause) {
    super("Unknown checksum algorithm", cause);
  }
}

package com.mipt.team4.cloud_storage_backend.antivirus.model.enums;

public enum ScanVerdict {
  UNKNOWN,
  SCANNING,
  CLEAN,
  INFECTED,
  EMPTY_FILE,
  TOO_LARGE,
  PASSWORD_PROTECTED,
  RESOURCE_EXHAUSTED,
  CONTENT_MISMATCH,
  ERROR;

  public boolean isCritical() {
    return this == INFECTED || this == RESOURCE_EXHAUSTED || this == CONTENT_MISMATCH;
  }
}

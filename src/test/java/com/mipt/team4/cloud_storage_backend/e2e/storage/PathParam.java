package com.mipt.team4.cloud_storage_backend.e2e.storage;

public enum PathParam {
  EXISTENT_FILE,
  NEW_FILE,
  EXISTENT_FOLDER;

  public boolean isExistent() {
    return this == EXISTENT_FILE || this == EXISTENT_FOLDER;
  }
}

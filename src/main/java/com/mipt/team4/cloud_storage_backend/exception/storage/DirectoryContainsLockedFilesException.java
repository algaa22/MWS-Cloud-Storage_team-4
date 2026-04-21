package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class DirectoryContainsLockedFilesException extends FileLockedException {

  public DirectoryContainsLockedFilesException(UUID directoryId) {
    super(
        String.format(
            "Directory [%s] cannot be deleted because it contains files currently under security scan or processing.",
            directoryId));
  }
}

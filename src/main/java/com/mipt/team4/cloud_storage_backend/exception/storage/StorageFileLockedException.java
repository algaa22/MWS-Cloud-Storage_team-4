package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.UUID;

public class StorageFileLockedException extends BaseStorageException {
  public StorageFileLockedException(UUID id, FileStatus expectedStatus, FileStatus actualStatus) {
    super(
        "File locked by other operation: id=%s, expectedStatus=%s, actualStatus=%s"
            .formatted(id, expectedStatus, actualStatus),
        HttpResponseStatus.CONFLICT);
  }
}

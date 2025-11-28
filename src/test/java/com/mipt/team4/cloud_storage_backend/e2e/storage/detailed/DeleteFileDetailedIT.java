package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.PathParam;
import io.netty.handler.codec.http.HttpMethod;

public class DeleteFileDetailedIT extends BaseDetailedFileIT {
  public DeleteFileDetailedIT() {
    super("/api/files?path=_", HttpMethod.DELETE.name(), PathParam.EXISTENT_FILE);
  }
}

package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.PathParam;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Tag;

@Tag("integration")
public class GetFileInfoDetailedIT extends BaseDetailedFileIT {

  public GetFileInfoDetailedIT() {
    super("/api/files/info?path=_", HttpMethod.GET.name(), PathParam.EXISTENT_FILE);
  }
}

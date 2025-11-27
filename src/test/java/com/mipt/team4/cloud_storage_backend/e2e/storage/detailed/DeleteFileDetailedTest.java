package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.storage.QueryType;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DeleteFileDetailedTest extends BaseDetailedFileE2ETest {
  public DeleteFileDetailedTest() {
    super("/api/files?path=_", HttpMethod.DELETE.name(), QueryType.SINGLE_FILE);
  }
}

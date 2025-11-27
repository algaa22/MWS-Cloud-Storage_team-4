package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.storage.QueryType;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class UploadFileDetailedTest extends BaseDetailedFileE2ETest {
  public UploadFileDetailedTest() {
    super("/api/files/upload?path=_", HttpMethod.POST.name(), QueryType.SINGLE_FILE);
  }

  @Test
  public void shouldUploadFile_AfterDelete() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);
  }
}

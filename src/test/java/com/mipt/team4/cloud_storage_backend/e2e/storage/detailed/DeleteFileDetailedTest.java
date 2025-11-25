package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsTestUtils;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.bouncycastle.pqc.crypto.lms.HSSKeyGenerationParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeleteFileDetailedTest extends BaseDetailedFileE2ETest {
  public DeleteFileDetailedTest() {
    super("/api/files?path=_", HttpMethod.DELETE.name(), true);
  }

  @Test
  public void shouldNotDeleteFolder() {
    // TODO
  }
}

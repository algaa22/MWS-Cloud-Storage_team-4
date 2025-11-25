package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsTestUtils;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.org.bouncycastle.pqc.crypto.lms.HSSKeyGenerationParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetFileInfoDetailedTest extends BaseDetailedFileE2ETest {
  public GetFileInfoDetailedTest() {
    super("/api/files/info?path=_", HttpMethod.GET.name(), true);
  }

  @ParameterizedTest
  @ValueSource(strings = {"dir/", "dir\\"})
  public void shouldNotGetInfo_WhenSpecifyFolder() throws IOException, InterruptedException {
    HttpResponse<String> response =
        FileOperationsTestUtils.sendGetFileInfoRequest(
            client, currentUserToken, "dir/");

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
    assertTrue(containsValidationError(response, "NOT_DIRECTORY"));
  }
}

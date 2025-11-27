package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.storage.QueryType;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsTestUtils;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UploadFileDetailedTest extends BaseDetailedFileE2ETest {
  public UploadFileDetailedTest() {
    super("/api/files/upload?path=_", HttpMethod.POST.name(), QueryType.SINGLE_FILE);
  }

  @Test
  public void shouldUploadFile_AfterDelete() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> response =
        FileOperationsTestUtils.sendDeleteFileRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);
    assertEquals(HttpStatus.SC_OK, response.statusCode());

    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);
  }
}

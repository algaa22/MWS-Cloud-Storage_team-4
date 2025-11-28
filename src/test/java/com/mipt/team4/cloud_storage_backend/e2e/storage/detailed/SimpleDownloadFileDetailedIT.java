package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.QueryType;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileSimpleTransferITUtils;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

public class SimpleDownloadFileDetailedIT extends BaseDetailedFileIT {
  public SimpleDownloadFileDetailedIT() {
    super("/api/files?path=_", HttpMethod.GET.name(), QueryType.SINGLE_FILE);
  }

  @Test
  public void shouldNotDownloadFile_AfterDelete() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> deleteResponse =
        FileOperationsITUtils.sendDeleteFileRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);
    assertEquals(HttpStatus.SC_OK, deleteResponse.statusCode());

    HttpResponse<byte[]> downloadResponse =
        FileSimpleTransferITUtils.sendDownloadRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH);
    assertEquals(HttpStatus.SC_BAD_REQUEST, downloadResponse.statusCode());
  }
}

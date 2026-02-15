package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.PathParam;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileSimpleTransferITUtils;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
public class SimpleUploadFileDetailedIT extends BaseDetailedFileIT {

  @Autowired private FileOperationsITUtils operationsITUtils;
  @Autowired private FileSimpleTransferITUtils transferITUtils;

  public SimpleUploadFileDetailedIT() {
    super("/api/files/upload?path=_", HttpMethod.POST.name(), PathParam.NEW_FILE);
  }

  @Test
  public void shouldUploadFile_AfterDelete() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> response =
        operationsITUtils.sendDeleteFileRequest(client, currentUserToken, DEFAULT_FILE_TARGET_PATH);
    assertEquals(HttpStatus.SC_OK, response.statusCode());

    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);
  }

  @Test
  public void shouldNotUploadEmptyFile() throws IOException, InterruptedException {
    HttpResponse<String> uploadResponse =
        transferITUtils.sendUploadRequest(
            client, currentUserToken, EMPTY_FILE_LOCAL_PATH, DEFAULT_FILE_TARGET_PATH, "");
    assertEquals(HttpStatus.SC_BAD_REQUEST, uploadResponse.statusCode());
  }
}

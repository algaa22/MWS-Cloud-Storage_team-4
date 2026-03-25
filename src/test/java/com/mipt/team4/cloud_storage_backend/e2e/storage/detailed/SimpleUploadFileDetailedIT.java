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
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("integration")
public class SimpleUploadFileDetailedIT extends BaseDetailedFileIT {

  @Autowired private FileOperationsITUtils operationsITUtils;
  @Autowired private FileSimpleTransferITUtils transferITUtils;

  public SimpleUploadFileDetailedIT() {
    super("/api/files/upload", HttpMethod.POST.name(), PathParam.NEW_ENTITY);
  }

  @Test
  public void shouldUploadFile_AfterDelete() throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);

    HttpResponse<String> response =
        operationsITUtils.sendDeleteFileRequest(client, currentUserToken, fileId, true);
    assertEquals(HttpStatus.SC_OK, response.statusCode());

    simpleUploadFile(DEFAULT_FILE_TARGET_NAME);
  }

  @Test
  public void shouldNotUploadEmptyFile() throws IOException, InterruptedException {
    HttpResponse<String> uploadResponse =
        transferITUtils.sendUploadRequest(
            client, currentUserToken, EMPTY_FILE_LOCAL_PATH, null, DEFAULT_FILE_TARGET_NAME, "");
    assertEquals(HttpStatus.SC_BAD_REQUEST, uploadResponse.statusCode());
  }
}

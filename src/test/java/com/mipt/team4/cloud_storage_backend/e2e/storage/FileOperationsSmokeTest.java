package com.mipt.team4.cloud_storage_backend.e2e.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.BaseE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.user.UserAuthUtils;
import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import java.io.IOException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FileOperationsSmokeTest extends BaseE2ETest {
  private static String testUserToken;

  @BeforeAll
  public static void beforeAll() {
    testUserToken =
        UserAuthUtils.sendRegisterTestUserRequest(
            client,
            "test@email.com",
            "deadlyparkourkillerdarkbrawlstarsassassinstalkersniper1998rus",
            "superpassword1488");
  }

  @Test
  public void shouldUploadAndDownloadFile_Simple() throws IOException, InterruptedException {
    final String testFilePath = "files/file.txt";

    HttpResponse<String> uploadResponse =
        SimpleFileTransferUtils.sendUploadFileRequest(client, testUserToken, testFilePath, "");

    assertEquals(HttpStatus.SC_OK, uploadResponse.statusCode());

    HttpResponse<byte[]> downloadResponse =
        SimpleFileTransferUtils.sendDownloadFileRequest(client, testUserToken, testFilePath);

    byte[] originalFile = FileLoader.getInputStream(testFilePath).readAllBytes();
    byte[] downloadedFile = downloadResponse.body();

    assertEquals(HttpStatus.SC_OK, downloadResponse.statusCode());
    assertArrayEquals(downloadedFile, originalFile);
  }

  @Test
  public void shouldUploadAndDownloadFile_Chunked() {}
}

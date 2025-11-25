package com.mipt.team4.cloud_storage_backend.e2e.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.BaseE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.user.UserAuthUtils;
import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FileOperationsSmokeTest extends BaseE2ETest {
  private static String testUserToken;

  // TODO:
  //  1. Если файл уже загружен

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
    final String SIMPLE_UPLOAD_FILE_PATH = "files/simple_upload.txt";

    HttpResponse<String> uploadResponse =
        SimpleFileTransferUtils.sendUploadFileRequest(
            client, testUserToken, SIMPLE_UPLOAD_FILE_PATH, "");

    assertEquals(HttpStatus.SC_OK, uploadResponse.statusCode());

    HttpResponse<byte[]> downloadResponse =
        SimpleFileTransferUtils.sendDownloadFileRequest(
            client, testUserToken, SIMPLE_UPLOAD_FILE_PATH);

    byte[] originalFile = FileLoader.getInputStream(SIMPLE_UPLOAD_FILE_PATH).readAllBytes();
    byte[] downloadedFile = downloadResponse.body();

    assertEquals(HttpStatus.SC_OK, downloadResponse.statusCode());
    assertArrayEquals(downloadedFile, originalFile);
  }

  @Test
  public void shouldUploadAndDownloadFile_Chunked()
      throws IOException, InterruptedException, URISyntaxException {
    final String CHUNKED_UPLOAD_FILE_PATH = "files/chunked_upload(cotik).jpg";

    byte[] fileData = FileLoader.getInputStream(CHUNKED_UPLOAD_FILE_PATH).readAllBytes();

    HttpResponse<String> response =
        ChunkedUploadTestUtils.createUploadRequest(
            client, testUserToken, "files/chunked_upload(cotik).jpg", fileData, "");

    HttpResponse<byte[]> downloadResponse =
            SimpleFileTransferUtils.sendDownloadFileRequest(
                    client, testUserToken, CHUNKED_UPLOAD_FILE_PATH);

    byte[] originalFile = FileLoader.getInputStream(CHUNKED_UPLOAD_FILE_PATH).readAllBytes();
    byte[] downloadedFile = downloadResponse.body();

    assertEquals(HttpStatus.SC_OK, downloadResponse.statusCode());
    assertArrayEquals(downloadedFile, originalFile);

    System.out.println(response.statusCode() + " " + response.body());
  }
}

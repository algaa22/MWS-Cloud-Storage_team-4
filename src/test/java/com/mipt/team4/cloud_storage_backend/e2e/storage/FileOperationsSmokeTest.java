package com.mipt.team4.cloud_storage_backend.e2e.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.BaseE2ETest;
import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class FileOperationsSmokeTest extends BaseE2ETest {
  private static String testUserToken;

  @BeforeAll
  public static void beforeAll() {
    testUserToken = sendRegisterTestUserRequest();
  }

  // TODO:
  //  1. Обычная загрузка файлов + обычное скачивание
  //  2. Чанковая загрузка файлов + чанковое скачивание

  @Test
  public void shouldUploadAndDownloadFile_Simple() throws IOException, InterruptedException {
    final String testFilePath = "files/file.txt";

    HttpResponse<String> uploadResponse =
        sendSimpleUploadFileRequest(testUserToken, testFilePath, "");

    assertEquals(HttpStatus.SC_OK, uploadResponse.statusCode());

    HttpResponse<byte[]> downloadResponse =
        sendSimpleDownloadFileRequest(testUserToken, testFilePath);

    byte[] originalFile = FileLoader.getInputStream(testFilePath).readAllBytes();
    byte[] downloadedFile = downloadResponse.body();

    assertEquals(HttpStatus.SC_OK, downloadResponse.statusCode());
    assertArrayEquals(downloadedFile, originalFile);
  }

  private HttpResponse<String> sendSimpleUploadFileRequest(
      String userToken, String filePath, String fileTags) throws IOException, InterruptedException {
    byte[] testFile = FileLoader.getInputStream(filePath).readAllBytes();

    HttpRequest request =
        createRequest("/api/files/upload?path=" + filePath)
            .header("X-Auth-Token", userToken)
            .header("X-File-Tags", fileTags)
            .POST(HttpRequest.BodyPublishers.ofByteArray(testFile))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<byte[]> sendSimpleDownloadFileRequest(String userToken, String filePath)
      throws IOException, InterruptedException {
    HttpRequest request =
        createRequest("/api/files?path=" + filePath)
            .header("X-Auth-Token", userToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
  }

  private static String sendRegisterTestUserRequest() {
    HttpRequest request =
        createRequest("/api/users/auth/register")
            .header("X-Auth-Email", "test@email.com")
            .header("X-Auth-Password", "superpassword1488")
            .header(
                "X-Auth-Username", "deadlyparkourkillerdarkbrawlstarsassassinstalkersniper1998rus")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();

      if (response.statusCode() != HttpStatus.SC_CREATED)
        throw new RuntimeException("Failed to register test user: " + responseBody);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(responseBody);

      return rootNode.get("token").asText();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}

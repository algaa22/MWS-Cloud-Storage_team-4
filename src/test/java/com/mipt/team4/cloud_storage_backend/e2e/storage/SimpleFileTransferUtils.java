package com.mipt.team4.cloud_storage_backend.e2e.storage;

import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SimpleFileTransferUtils {
  public static HttpResponse<String> sendUploadFileRequest(
      HttpClient client, String userToken, String filePath, String fileTags)
      throws IOException, InterruptedException {
    byte[] testFile = FileLoader.getInputStream(filePath).readAllBytes();

    HttpRequest request =
        TestUtils.createRequest("/api/files/upload?path=" + filePath)
            .header("X-Auth-Token", userToken)
            .header("X-File-Tags", fileTags)
            .POST(HttpRequest.BodyPublishers.ofByteArray(testFile))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<byte[]> sendDownloadFileRequest(
      HttpClient client, String userToken, String filePath)
      throws IOException, InterruptedException {
    HttpRequest request =
        TestUtils.createRequest("/api/files?path=" + filePath)
            .header("X-Auth-Token", userToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
  }
}

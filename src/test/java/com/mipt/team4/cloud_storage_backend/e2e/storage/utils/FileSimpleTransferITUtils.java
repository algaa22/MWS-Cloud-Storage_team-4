package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.utils.FileLoader;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FileSimpleTransferITUtils {
  public static HttpResponse<String> sendUploadRequest(
      HttpClient client,
      String userToken,
      String localFilePath,
      String targetFilePath,
      String fileTags)
      throws IOException, InterruptedException {
    byte[] testFile = FileLoader.getInputStream(localFilePath).readAllBytes();

    HttpRequest request =
        TestUtils.createRequest("/api/files/upload?path=" + targetFilePath)
            .header("X-Auth-Token", userToken)
            .header("X-File-Tags", fileTags)
            .POST(HttpRequest.BodyPublishers.ofByteArray(testFile))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<byte[]> sendDownloadRequest(
      HttpClient client, String userToken, String targetFilePath)
      throws IOException, InterruptedException {
    HttpRequest request =
        TestUtils.createRequest("/api/files?path=" + targetFilePath)
            .header("X-Auth-Token", userToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
  }
}

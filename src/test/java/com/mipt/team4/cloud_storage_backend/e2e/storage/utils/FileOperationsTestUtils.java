package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FileOperationsTestUtils {
  public static HttpResponse<String> sendGetFilePathsListRequest(
      HttpClient client, String userToken) throws IOException, InterruptedException {
    HttpRequest request =
        TestUtils.createRequest("/api/files").header("X-Auth-Token", userToken).GET().build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendGetFileInfoRequest(
      HttpClient client, String userToken, String targetFilePath)
      throws IOException, InterruptedException {
    HttpRequest request =
        TestUtils.createRequest("/api/files/info?path=" + targetFilePath)
            .header("X-Auth-Token", userToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendDeleteFileRequest(
      HttpClient client, String userToken, String targetFilePath) throws IOException, InterruptedException {
    HttpRequest request =
            TestUtils.createRequest("/api/files?path=" + targetFilePath)
                    .header("X-Auth-Token", userToken)
                    .DELETE()
                    .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendChangeFilePathRequest(
      HttpClient client, String userToken, String oldTargetFilePath, String newTargetFilePath)
      throws IOException, InterruptedException {
    return sendChangeFileMetadataRequest(
        client, userToken, oldTargetFilePath, "X-File-New-Path", newTargetFilePath);
  }

  public static HttpResponse<String> sendChangeFileVisibilityRequest(
      HttpClient client, String userToken, String targetFilePath, String visibility)
      throws IOException, InterruptedException {
    return sendChangeFileMetadataRequest(
        client, userToken, targetFilePath, "X-File-Visibility", visibility);
  }

  public static HttpResponse<String> sendChangeFileTagsRequest(
      HttpClient client, String userToken, String targetFilePath, String fileTags)
      throws IOException, InterruptedException {
    return sendChangeFileMetadataRequest(
        client, userToken, targetFilePath, "X-File-Tags", fileTags);
  }

  private static HttpResponse<String> sendChangeFileMetadataRequest(
      HttpClient client, String userToken, String targetFilePath, String header, String value)
      throws IOException, InterruptedException {
    HttpRequest request =
        TestUtils.createRequest("/api/files?path=" + targetFilePath)
            .header("X-Auth-Token", userToken)
            .header(header, value)
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}

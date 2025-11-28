package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FileOperationsITUtils {
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
      HttpClient client, String userToken, String targetFilePath)
      throws IOException, InterruptedException {
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
      HttpClient client, String userToken, String targetFilePath, String newVisibility)
      throws IOException, InterruptedException {
    return sendChangeFileMetadataRequest(
        client, userToken, targetFilePath, "X-File-New-Visibility", newVisibility);
  }

  public static HttpResponse<String> sendChangeFileTagsRequest(
      HttpClient client, String userToken, String targetFilePath, String newTags)
      throws IOException, InterruptedException {
    return sendChangeFileMetadataRequest(
        client, userToken, targetFilePath, "X-File-New-Tags", newTags);
  }

  public static HttpResponse<String> sendChangeFileMetadataRequest(
      HttpClient client,
      String userToken,
      String targetFilePath,
      String newPath,
      String newVisibility,
      String newTags)
      throws IOException, InterruptedException {
    HttpRequest request =
        TestUtils.createRequest("/api/files?path=" + targetFilePath)
            .header("X-Auth-Token", userToken)
            .header("X-File-New-Path", newPath)
            .header("X-File-New-Visibility", newVisibility)
            .header("X-File-New-Tags", newTags)
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
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

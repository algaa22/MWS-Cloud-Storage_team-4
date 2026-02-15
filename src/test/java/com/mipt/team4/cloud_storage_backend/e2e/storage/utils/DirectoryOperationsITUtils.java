package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import com.mipt.team4.cloud_storage_backend.utils.ITUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

@Component
public class DirectoryOperationsITUtils {
  private final ITUtils itUtils;

  public DirectoryOperationsITUtils(ITUtils itUtils) {
    this.itUtils = itUtils;
  }

  public HttpResponse<String> sendCreateDirectoryRequest(
      HttpClient client, String userToken, String directoryPath)
      throws IOException, InterruptedException {
    HttpRequest request =
        itUtils.createRequest("/api/directories?path=" + directoryPath)
            .header("X-Auth-Token", userToken)
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendDeleteDirectoryRequest(
      HttpClient client, String userToken, String directoryPath)
      throws IOException, InterruptedException {
    HttpRequest request =
        itUtils.createRequest("/api/directories?path=" + directoryPath)
            .header("X-Auth-Token", userToken)
            .DELETE()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendChangeDirectoryPathRequest(
      HttpClient client,
      String currentUserToken,
      String oldDirectoryPath,
      String newDirectoryPath) throws IOException, InterruptedException {
    HttpRequest request =
        itUtils.createRequest(
                "/api/directories?from=%s&to=%s".formatted(oldDirectoryPath, newDirectoryPath))
            .header("X-Auth-Token", currentUserToken)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}

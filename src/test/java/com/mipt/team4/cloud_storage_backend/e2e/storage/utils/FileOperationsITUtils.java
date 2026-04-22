package com.mipt.team4.cloud_storage_backend.e2e.storage.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.utils.ITUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class FileOperationsITUtils {
  public final ITUtils itUtils;

  public boolean fileListContainsFileNames(
      HttpClient client,
      String userToken,
      List<String> fileNames,
      boolean includeDirectories,
      boolean recursive,
      UUID searchDirectoryId)
      throws IOException, InterruptedException {
    HttpResponse<String> response =
        sendGetFilePathsListRequest(
            client, userToken, includeDirectories, recursive, searchDirectoryId);
    assertEquals(HttpStatus.SC_OK, response.statusCode());

    JsonNode filesNode = itUtils.getRootNodeFromResponse(response).get("page").get("content");
    List<String> responseFiles = new ArrayList<>();

    for (int i = 0; i < filesNode.size(); i++) {
      JsonNode fileNode = filesNode.get(i);
      JsonNode fileNameNode = fileNode.get("name");

      responseFiles.add(fileNameNode.asText());
    }

    return responseFiles.containsAll(fileNames);
  }

  public HttpResponse<String> sendGetFilePathsListRequest(
      HttpClient client,
      String userToken,
      boolean includeDirectories,
      boolean recursive,
      UUID searchDirectoryId)
      throws IOException, InterruptedException {
    String endpoint =
        itUtils.fillQuery(
            "/api/files/list?includeDirectories=%s&recursive=%s&page=%s&size=%s&direction=%s&sort_by=%s",
            includeDirectories, recursive, 0, 100, "asc", "name");

    if (searchDirectoryId != null) {
      endpoint += itUtils.fillQuery("&parentId=%s", searchDirectoryId);
    }

    HttpRequest request =
        itUtils.createRequest(endpoint).header("X-Auth-Token", userToken).GET().build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendGetFileInfoRequest(
      HttpClient client, String userToken, UUID targetFileId)
      throws IOException, InterruptedException {
    HttpRequest request =
        itUtils
            .createRequest(itUtils.fillQuery("/api/files/info?id=%s", targetFileId))
            .header("X-Auth-Token", userToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendDeleteFileRequest(
      HttpClient client, String userToken, UUID targetFileId, boolean isPermanent)
      throws IOException, InterruptedException {
    HttpRequest request =
        itUtils
            .createRequest(
                itUtils.fillQuery("/api/files?id=%s&permanent=%s", targetFileId, isPermanent))
            .header("X-Auth-Token", userToken)
            .DELETE()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendRestoreFileRequest(
      HttpClient client, String userToken, UUID fileId) throws IOException, InterruptedException {

    HttpRequest request =
        itUtils
            .createRequest(itUtils.fillQuery("/api/files/restore?id=%s", fileId))
            .header("X-Auth-Token", userToken)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendChangeFilePathRequest(
      HttpClient client, String userToken, UUID targetFileId, String newTargetFileName)
      throws IOException, InterruptedException {
    HttpRequest request =
        itUtils
            .createRequest(
                itUtils.fillQuery("/api/files?id=%s&newName=%s", targetFileId, newTargetFileName))
            .header("X-Auth-Token", userToken)
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendChangeFileVisibilityRequest(
      HttpClient client, String userToken, UUID targetFileId, String newVisibility)
      throws IOException, InterruptedException {
    return sendChangeFileMetadataRequest(
        client, userToken, targetFileId, "X-File-New-Visibility", newVisibility);
  }

  public HttpResponse<String> sendChangeFileTagsRequest(
      HttpClient client, String userToken, UUID targetFileId, String newTags)
      throws IOException, InterruptedException {
    return sendChangeFileMetadataRequest(
        client, userToken, targetFileId, "X-File-New-Tags", newTags);
  }

  public HttpResponse<String> sendChangeFileMetadataRequest(
      HttpClient client,
      String userToken,
      UUID targetId,
      String newTargetPath,
      String newVisibility,
      String newTags)
      throws IOException, InterruptedException {
    HttpRequest request =
        itUtils
            .createRequest(
                itUtils.fillQuery("/api/files?id=%s&newName=%s", targetId, newTargetPath))
            .header("X-Auth-Token", userToken)
            .header("X-File-New-Visibility", newVisibility)
            .header("X-File-New-Tags", newTags)
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> sendChangeFileMetadataRequest(
      HttpClient client, String userToken, UUID targetFileId, String header, String value)
      throws IOException, InterruptedException {
    HttpRequest request =
        itUtils
            .createRequest(itUtils.fillQuery("/api/files?id=%s", targetFileId))
            .header("X-Auth-Token", userToken)
            .header(header, value)
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}

package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.PathParam;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class ChangeFileMetadataDetailedIT extends BaseDetailedFileIT {
  private static final String NEW_PATH = "new_file";
  private static final String NEW_VISIBILITY = "public";
  private static final String NEW_TAGS = "1,2,3";

  public ChangeFileMetadataDetailedIT() {
    super("/api/files?path=_", HttpMethod.PUT.name(), PathParam.EXISTENT_FILE);
  }

  @Override
  @Test
  public void shouldNotDoX_WhenSpecifyFolder() throws IOException, InterruptedException {
    HttpResponse<String> response =
            client.send(
                    createRawRequestWithToken(currentUserToken,  "/api/files?path=dir/"),
                    HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
    assertTrue(containsValidationError(response, "Old file path"));
  }

  @Test
  public void shouldNotDoX_WhenSpecifyFolderInNewPath() throws IOException, InterruptedException {
    HttpResponse<String> response =
            client.send(
                    TestUtils.createRequest("/api/files?path=_")
                            .header("X-Auth-Token", currentUserToken)
                            .header("X-File-Tags", "")
                            .header("X-File-New-Path", "dir/")
                            .PUT(HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
    assertTrue(containsValidationError(response, "New file path"));
  }

  @Test
  public void shouldNotRenameToExistingFilename() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);
    simpleUploadFile(NEW_PATH);

    HttpResponse<String> response =
        FileOperationsITUtils.sendChangeFilePathRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, NEW_PATH);
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());

    JsonNode rootNode = TestUtils.getRootNodeFromResponse(response);
    assertTrue(rootNode.get("message").asText().contains("already exists"));
  }

  @Test
  public void shouldUpdateAllFields_ByOneRequest() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> response =
        FileOperationsITUtils.sendChangeFileMetadataRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, NEW_PATH, NEW_VISIBILITY, NEW_TAGS);
    assertEquals(HttpStatus.SC_OK, response.statusCode());

    assertFileInfoChanged(DEFAULT_FILE_TARGET_PATH, NEW_PATH, NEW_VISIBILITY, NEW_TAGS);
  }

  @Test
  public void shouldNotUpdateFile_WhenVisibilityIsInvalid() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> response =
            FileOperationsITUtils.sendChangeFileVisibilityRequest(
                    client, currentUserToken, DEFAULT_FILE_TARGET_PATH, "asdsad");
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
    containsValidationError(response, "Visibility");
  }

  @Test
  public void shouldUpdateAllFields_ByMultipleRequest() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    HttpResponse<String> changeFileVisibilityResponse =
        FileOperationsITUtils.sendChangeFileVisibilityRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, NEW_VISIBILITY);
    assertEquals(HttpStatus.SC_OK, changeFileVisibilityResponse.statusCode());

    HttpResponse<String> changeFileTagsResponse =
        FileOperationsITUtils.sendChangeFileTagsRequest(
            client, currentUserToken, DEFAULT_FILE_TARGET_PATH, NEW_TAGS);
    assertEquals(HttpStatus.SC_OK, changeFileTagsResponse.statusCode());

    HttpResponse<String> changeFilePathResponse =
            FileOperationsITUtils.sendChangeFilePathRequest(
                    client, currentUserToken, DEFAULT_FILE_TARGET_PATH, NEW_PATH);
    assertEquals(HttpStatus.SC_OK, changeFilePathResponse.statusCode());

    assertFileInfoChanged(DEFAULT_FILE_TARGET_PATH, NEW_PATH, NEW_VISIBILITY, NEW_TAGS);
  }

  private void assertFileInfoChanged(
      String oldPath, String newPath, String newVisibility, String newTags)
      throws IOException, InterruptedException {
    assertFileExistsIs(false, oldPath);
    assertFileExistsIs(true, newPath);
    assertFileInfoMatches(newPath, newVisibility, newTags);
  }
}

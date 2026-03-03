package com.mipt.team4.cloud_storage_backend.e2e.storage.detailed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.storage.BaseDetailedFileIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.PathParam;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

@Tag("integration")
public class ChangeFileMetadataDetailedIT extends BaseDetailedFileIT {
  private static final String NEW_NAME = "new_file";
  private static final String NEW_VISIBILITY = "public";
  private static final String NEW_TAGS = "1,2,3";

  @Autowired private FileOperationsITUtils fileOperationsITUtils;

  public ChangeFileMetadataDetailedIT() {
    super( "/api/files", HttpMethod.PUT.name(), PathParam.EXISTENT_FILE);
  }

  @Test
  public void shouldNotRenameToExistingFilename() throws IOException, InterruptedException {
    UUID firstFileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);
    simpleUploadFile(NEW_NAME);

    HttpResponse<String> response =
        fileOperationsITUtils.sendChangeFilePathRequest(
            client, currentUserToken, firstFileId, NEW_NAME);
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());

    JsonNode rootNode = itUtils.getRootNodeFromResponse(response);
    assertTrue(rootNode.get("message").asText().contains("already exists"));
  }

  @Test
  public void shouldUpdateAllFields_ByOneRequest() throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);

    HttpResponse<String> response =
        fileOperationsITUtils.sendChangeFileMetadataRequest(
            client, currentUserToken, fileId, NEW_NAME, NEW_VISIBILITY, NEW_TAGS);
    assertEquals(HttpStatus.SC_OK, response.statusCode());

    assertFileInfoMatches(fileId, NEW_NAME,  NEW_VISIBILITY, NEW_TAGS);
  }

  @Test
  public void shouldNotUpdateFile_WhenVisibilityIsInvalid()
      throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);

    HttpResponse<String> response =
        fileOperationsITUtils.sendChangeFileVisibilityRequest(
            client, currentUserToken, fileId, "asdsad");
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
    containsValidationError(response, "Visibility");
  }

  @Test
  public void shouldUpdateAllFields_ByMultipleRequest() throws IOException, InterruptedException {
    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);

    HttpResponse<String> changeFileVisibilityResponse =
        fileOperationsITUtils.sendChangeFileVisibilityRequest(
            client, currentUserToken, fileId, NEW_VISIBILITY);
    assertEquals(HttpStatus.SC_OK, changeFileVisibilityResponse.statusCode());

    HttpResponse<String> changeFileTagsResponse =
        fileOperationsITUtils.sendChangeFileTagsRequest(
            client, currentUserToken, fileId, NEW_TAGS);
    assertEquals(HttpStatus.SC_OK, changeFileTagsResponse.statusCode());

    HttpResponse<String> changeFilePathResponse =
        fileOperationsITUtils.sendChangeFilePathRequest(
            client, currentUserToken, fileId, NEW_NAME);
    assertEquals(HttpStatus.SC_OK, changeFilePathResponse.statusCode());

    assertFileInfoMatches(fileId, NEW_NAME, NEW_VISIBILITY, NEW_TAGS);
  }
}

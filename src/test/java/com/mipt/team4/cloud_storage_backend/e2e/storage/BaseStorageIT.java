package com.mipt.team4.cloud_storage_backend.e2e.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.BaseIT;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileSimpleTransferITUtils;
import com.mipt.team4.cloud_storage_backend.e2e.user.utils.UserAuthUtils;
import com.mipt.team4.cloud_storage_backend.utils.ITUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestConstants;
import java.io.IOException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public abstract class BaseStorageIT extends BaseIT {

  protected static final String EMPTY_FILE_LOCAL_PATH = "files/empty_file";
  protected static final String DEFAULT_FILE_TARGET_PATH = "file";
  protected static final String DEFAULT_DIRECTORY_PATH = "dir1/dir2/";

  @Autowired protected FileSimpleTransferITUtils transferITUtils;
  @Autowired protected FileOperationsITUtils operationsITUtils;
  @Autowired protected UserAuthUtils userAuthUtils;
  @Autowired protected ITUtils itUtils;

  protected String currentUserToken;

  @BeforeEach
  public void beforeEach() throws IOException, InterruptedException {
    currentUserToken = userAuthUtils.sendRegisterRandomUserRequest(client);
  }

  protected void assertFileNotFound(HttpResponse<String> response) throws IOException {
    JsonNode rootNode = itUtils.getRootNodeFromResponse(response);

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
    assertTrue(rootNode.get("message").asText().contains("not found"));
  }

  protected void simpleUploadFile(String targetFilePath) throws IOException, InterruptedException {
    simpleUploadFile(TestConstants.SMALL_FILE_LOCAL_PATH, targetFilePath, "");
  }

  protected void simpleUploadFile(String localFilePath, String targetFilePath, String fileTags)
      throws IOException, InterruptedException {
    HttpResponse<String> uploadResponse =
        transferITUtils.sendUploadRequest(
            client, currentUserToken, localFilePath, targetFilePath, fileTags);
    assertEquals(HttpStatus.SC_OK, uploadResponse.statusCode());
  }

  protected void assertFileExistsIs(boolean exists, String targetFilePath)
      throws IOException, InterruptedException {
    HttpResponse<String> response =
        operationsITUtils.sendGetFileInfoRequest(client, currentUserToken, targetFilePath);

    if (exists) {
      assertEquals(HttpStatus.SC_OK, response.statusCode());
    } else {
      assertFileNotFound(response);
    }
  }

  protected void assertFileInfoMatches(
      String targetPath, String expectedVisibility, String expectedTags)
      throws IOException, InterruptedException {
    HttpResponse<String> fileInfoResponse =
        operationsITUtils.sendGetFileInfoRequest(client, currentUserToken, targetPath);
    assertEquals(HttpStatus.SC_OK, fileInfoResponse.statusCode());

    JsonNode rootNode = itUtils.getRootNodeFromResponse(fileInfoResponse);

    if (expectedVisibility != null) {
      assertEquals(expectedVisibility, rootNode.get("Visibility").asText());
    }

    if (expectedTags != null) {
      assertEquals(expectedTags, rootNode.get("Tags").asText());
    }
  }
}

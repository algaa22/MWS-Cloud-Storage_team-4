package com.mipt.team4.cloud_storage_backend.e2e.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.BaseE2ETest;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileOperationsTestUtils;
import com.mipt.team4.cloud_storage_backend.e2e.storage.utils.FileSimpleTransferTestUtils;
import com.mipt.team4.cloud_storage_backend.e2e.user.UserAuthUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public abstract class BaseFileE2ETest extends BaseE2ETest {
  protected static final String SMALL_FILE_LOCAL_PATH = "files/small_file.txt";
  protected static final String BIG_FILE_LOCAL_PATH = "files/big_file.jpg";
  protected static final String DEFAULT_FILE_TARGET_PATH = "file";

  protected String currentUserToken;

  @BeforeEach
  public void beforeEach() {
    currentUserToken = String.valueOf(UserAuthUtils.sendRegisterRandomUserRequest(client));
  }

  protected void assertFileNotFound(HttpResponse<String> response) throws IOException {
    JsonNode rootNode = TestUtils.getRootNodeFromResponse(response);

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
    assertTrue(rootNode.get("message").asText().contains("not found"));
  }

  protected void simpleUploadFile(String targetFilePath) throws IOException, InterruptedException {
    simpleUploadFile(targetFilePath, "");
  }

  protected void simpleUploadFile(String targetFilePath, String fileTags)
          throws IOException, InterruptedException {
    HttpResponse<String> uploadResponse =
            FileSimpleTransferTestUtils.sendUploadFileRequest(
                    client, currentUserToken, SMALL_FILE_LOCAL_PATH, targetFilePath, fileTags);
    assertEquals(HttpStatus.SC_OK, uploadResponse.statusCode());
  }

  protected void assertFileExistsIs(boolean exists, String targetFilePath)
          throws IOException, InterruptedException {
    HttpResponse<String> response =
            FileOperationsTestUtils.sendGetFileInfoRequest(client, currentUserToken, targetFilePath);

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
            FileOperationsTestUtils.sendGetFileInfoRequest(client, currentUserToken, targetPath);
    assertEquals(HttpStatus.SC_OK, fileInfoResponse.statusCode());

    JsonNode rootNode = TestUtils.getRootNodeFromResponse(fileInfoResponse);

    if (expectedVisibility != null) {
      assertEquals(expectedVisibility, rootNode.get("Visibility").asText());
    }

    if (expectedTags != null) {
      assertEquals(expectedTags, rootNode.get("Tags").asText());
    }
  }
}

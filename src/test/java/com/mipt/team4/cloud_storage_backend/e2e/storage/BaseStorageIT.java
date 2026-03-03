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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public abstract class BaseStorageIT extends BaseIT {

  protected static final String EMPTY_FILE_LOCAL_PATH = "files/empty_file";
  protected static final String DEFAULT_FILE_TARGET_NAME = "file";
  protected static final String DEFAULT_DIRECTORY_NAME = "defaultDirectoryName";

  @Autowired protected FileSimpleTransferITUtils transferITUtils;
  @Autowired protected FileOperationsITUtils operationsITUtils;
  @Autowired protected UserAuthUtils userAuthUtils;
  @Autowired protected ITUtils itUtils;

  protected String currentUserToken;

  @BeforeEach
  public void beforeEach() throws IOException, InterruptedException {
    currentUserToken = userAuthUtils.sendRegisterRandomUserRequest(client);
  }

  protected void assertFileNotFound(String userToken, UUID fileId)
      throws IOException, InterruptedException {
    HttpResponse<String> response =
        operationsITUtils.sendGetFileInfoRequest(client, userToken, fileId);
    assertEquals(HttpStatus.SC_NOT_FOUND, response.statusCode());

    JsonNode rootNode = itUtils.getRootNodeFromResponse(response);
    assertTrue(rootNode.get("message").asText().contains("not found"));
  }

  protected UUID simpleUploadFile(String targetFileName) throws IOException, InterruptedException {
    return simpleUploadFile(targetFileName, "");
  }

  protected UUID simpleUploadFile(String targetFileName, String fileTags)
      throws IOException, InterruptedException {
    return simpleUploadFile(null, targetFileName, fileTags);
  }

  protected UUID simpleUploadFile(UUID parentId, String targetFileName, String fileTags)
      throws IOException, InterruptedException {
    HttpResponse<String> uploadResponse =
        transferITUtils.sendUploadRequest(
            client,
            currentUserToken,
            TestConstants.SMALL_FILE_LOCAL_PATH,
            parentId,
            targetFileName,
            fileTags);

    assertEquals(HttpStatus.SC_CREATED, uploadResponse.statusCode());

    return itUtils.extractIdFromResponse(uploadResponse);
  }

  protected void assertFileInfoMatches(
      UUID targetFileId, String expectedName, String expectedVisibility, String expectedTags)
      throws IOException, InterruptedException {
    HttpResponse<String> fileInfoResponse =
        operationsITUtils.sendGetFileInfoRequest(client, currentUserToken, targetFileId);
    assertEquals(HttpStatus.SC_OK, fileInfoResponse.statusCode());

    JsonNode rootNode = itUtils.getRootNodeFromResponse(fileInfoResponse);

    if (expectedName != null) {
      assertEquals(expectedName, rootNode.get("Name").asText());
    }

    if (expectedVisibility != null) {
      assertEquals(expectedVisibility, rootNode.get("Visibility").asText());
    }

    if (expectedTags != null) {
      assertEquals(expectedTags, rootNode.get("Tags").asText());
    }
  }
}

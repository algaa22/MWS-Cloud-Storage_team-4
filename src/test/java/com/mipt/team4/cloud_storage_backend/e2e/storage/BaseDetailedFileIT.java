package com.mipt.team4.cloud_storage_backend.e2e.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.user.utils.UserAuthUtils;
import com.mipt.team4.cloud_storage_backend.utils.ITUtils;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public abstract class BaseDetailedFileIT extends BaseStorageIT {

  @Autowired protected ITUtils itUtils;
  @Autowired protected UserAuthUtils userAuthUtils;

  private final boolean usePagination;
  private final PathParam pathParam;
  private final String rawEndpoint;
  private final String method;

  public BaseDetailedFileIT(
      String rawEndpoint, String method, PathParam pathParam, boolean usePagination) {
    this.rawEndpoint = rawEndpoint;
    this.method = method;
    this.pathParam = pathParam;
    this.usePagination = usePagination;
  }

  // TODO: тест на отсутствие хедеров, параметром
  // TODO: тест на expired токен

  @Test
  public void shouldEnforceUsersIsolation() throws IOException, InterruptedException {
    if (!pathParam.isExistent()) {
      return;
    }

    UUID fileId = simpleUploadFile(DEFAULT_FILE_TARGET_NAME);
    String otherUserToken = userAuthUtils.sendRegisterRandomUserRequest(client);

    if (pathParam == PathParam.EXISTENT_FILE) {
      assertFileNotFound(otherUserToken, fileId);
    } else if (pathParam == PathParam.EXISTENT_DIRECTORY) {
      HttpResponse<String> ownerResponse =
          client.send(
              createRawRequestWithToken(currentUserToken), HttpResponse.BodyHandlers.ofString());

      HttpResponse<String> otherUserResponse =
          client.send(
              createRawRequestWithToken(otherUserToken), HttpResponse.BodyHandlers.ofString());

      assertNotEquals(ownerResponse.body(), otherUserResponse.body());
    }
  }

  @Test
  public void shouldNotDoX_WhenSpecifyNotExistentFile() throws IOException, InterruptedException {
    if (pathParam != PathParam.EXISTENT_FILE) {
      return;
    }

    assertFileNotFound(currentUserToken, UUID.randomUUID());
  }

  @Test
  public void shouldReturnValidError_WhenSpecifyNonExistentToken()
      throws IOException, InterruptedException {
    assertTokenIsInvalid(createRawRequestWithToken("42"));
  }

  @Test
  public void shouldReturnValidError_WhenSpecifyEmptyToken()
      throws IOException, InterruptedException {
    assertTokenIsInvalid(createRawRequestWithToken(""));
  }

  private void assertTokenIsInvalid(HttpRequest request) throws IOException, InterruptedException {
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(HttpStatus.SC_UNAUTHORIZED, response.statusCode());

    JsonNode messageJsonNode = itUtils.getRootNodeFromResponse(response).get("message");
    assertNotNull(messageJsonNode);
    assertTrue(messageJsonNode.asText().contains("token"));
  }

  protected boolean containsValidationError(HttpResponse<String> response, String validationField)
      throws IOException {
    JsonNode messageJsonNode = itUtils.getRootNodeFromResponse(response).get("message");
    assertNotNull(messageJsonNode);

    JsonNode detailsNode = itUtils.getRootNodeFromResponse(response).get("details");
    assertNotNull(detailsNode);

    boolean hasTargetValidationError = false;

    for (Iterator<JsonNode> it = detailsNode.elements(); it.hasNext(); ) {
      JsonNode detailNode = it.next();

      if (detailNode.get("field").asText().equalsIgnoreCase(validationField)) {
        hasTargetValidationError = true;
        break;
      }
    }

    return hasTargetValidationError;
  }

  protected HttpRequest createRawRequestWithToken(String userToken) {
    String endpoint = rawEndpoint + "?id=%s";

    if (pathParam == PathParam.NEW_ENTITY) {
      endpoint += "&name=_";
    }

    if (usePagination) {
      endpoint += "&page=%s&size=%s&direction=%s&sort_by=%s".formatted(0, 100, "asc", "name");
    }

    return itUtils
        .createRequest(itUtils.fillQuery(endpoint, UUID.randomUUID()))
        .header("X-Auth-Token", userToken)
        .header("X-File-Tags", "")
        .header("X-File-New-Tags", "")
        .method(method, HttpRequest.BodyPublishers.noBody())
        .build();
  }
}

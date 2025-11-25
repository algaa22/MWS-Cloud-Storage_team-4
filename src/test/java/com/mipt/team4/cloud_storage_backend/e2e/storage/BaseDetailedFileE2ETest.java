package com.mipt.team4.cloud_storage_backend.e2e.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.user.UserAuthUtils;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BaseDetailedFileE2ETest extends BaseFileE2ETest {
  private final boolean isQueryForSingleFile;
  private final String endpoint;
  private final String method;

  public BaseDetailedFileE2ETest(String rawEndpoint, String method, boolean isQueryForSingleFile) {
    this.endpoint = rawEndpoint;
    this.method = method;
    this.isQueryForSingleFile = isQueryForSingleFile;
  }

  // TODO: тест на отсутствие хедеров, параметром
  // TODO: тест на expired токен

  @Test
  public void shouldEnforceUsersIsolation() throws IOException, InterruptedException {
    simpleUploadFile(DEFAULT_FILE_TARGET_PATH);

    String otherUserToken = UserAuthUtils.sendRegisterRandomUserRequest(client);
    HttpResponse<String> otherUserResponse =
        client.send(
            createRawRequestWithToken(otherUserToken), HttpResponse.BodyHandlers.ofString());

    if (isQueryForSingleFile) {
      assertFileNotFound(otherUserResponse);
    } else {
      HttpResponse<String> ownerResponse =
          client.send(
              createRawRequestWithToken(currentUserToken), HttpResponse.BodyHandlers.ofString());

      assertNotEquals(ownerResponse.body(), otherUserResponse.body());
    }
  }

  @Test
  public void shouldNotDoX_WhenSpecifyNotExistentFile() throws IOException, InterruptedException {
    if (!isQueryForSingleFile) return;

    simpleUploadFile("asdfghjklasdasdflhgehsagjak");

    HttpResponse<String> response =
        client.send(
            createRawRequestWithToken(currentUserToken), HttpResponse.BodyHandlers.ofString());

    assertFileNotFound(response);
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
    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());

    assertTrue(containsValidationError(response, "VALID_TOKEN"));
  }

  protected boolean containsValidationError(HttpResponse<String> response, String validationCode) throws IOException {
    JsonNode messageJsonStrNode = TestUtils.getRootNodeFromResponse(response).get("message");
    assertNotNull(messageJsonStrNode);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode messageNode = mapper.readTree(messageJsonStrNode.asText());
    assertTrue(messageNode.has("details"));

    boolean hasTargetValidationError = false;

    for (Iterator<JsonNode> it = messageNode.get("details").elements(); it.hasNext(); ) {
      JsonNode detailNode = it.next();

      if (detailNode.get("code").asText().equals(validationCode)) {
        hasTargetValidationError = true;
        break;
      }
    }

    return hasTargetValidationError;
  }

  private HttpRequest createRawRequestWithToken(String userToken) {
    return TestUtils.createRequest(endpoint)
        .header("X-Auth-Token", userToken)
        .method(method, HttpRequest.BodyPublishers.noBody())
        .build();
  }
}

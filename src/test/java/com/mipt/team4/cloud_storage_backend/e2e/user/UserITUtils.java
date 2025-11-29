package com.mipt.team4.cloud_storage_backend.e2e.user;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class UserITUtils {

  public static HttpResponse<String> sendLoginRequest(
      HttpClient client, String email, String password) throws IOException, InterruptedException {

    HttpRequest request =
        TestUtils.createRequest("/api/users/auth/login")
            .header("X-Auth-Email", email)
            .header("X-Auth-Password", password)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendRefreshTokenRequest(HttpClient client, String refreshToken)
      throws IOException, InterruptedException {
    String json = String.format("{\"refreshToken\":\"%s\"}", refreshToken);

    HttpRequest request =
        TestUtils.createRequest("/api/users/refresh")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendUserInfoRequest(HttpClient client, String accessToken)
      throws IOException, InterruptedException {

    HttpRequest request =
        TestUtils.createRequest("/api/users/info")
            .header("X-Auth-Token", accessToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendUpdateUserInfoRequest(
      HttpClient client, String accessToken, String newName)
      throws IOException, InterruptedException {

    HttpRequest request =
        TestUtils.createRequest("/api/users/update")
            .header("X-Auth-Token", accessToken)
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendLogoutRequest(HttpClient client, String accessToken)
      throws IOException, InterruptedException {
    HttpRequest request =
        TestUtils.createRequest("/api/users/auth/logout")
            .header("X-Auth-Token", accessToken)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static String extractAccessToken(HttpResponse<String> response) throws IOException {
    JsonNode root = TestUtils.getRootNodeFromResponse(response);
    return root.get("token").asText();
  }

  public static String extractRefreshToken(HttpResponse<String> response) throws IOException {
    JsonNode root = TestUtils.getRootNodeFromResponse(response);
    return root.get("refreshToken").asText();
  }
}

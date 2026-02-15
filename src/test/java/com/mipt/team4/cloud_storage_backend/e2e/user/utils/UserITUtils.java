package com.mipt.team4.cloud_storage_backend.e2e.user.utils;

import com.mipt.team4.cloud_storage_backend.utils.ITUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class UserITUtils {
  private final ITUtils itUtils;

  public HttpResponse<String> sendLoginRequest(
      HttpClient client, String email, String password) throws IOException, InterruptedException {

    HttpRequest request =
        itUtils.createRequest("/api/users/auth/login")
            .header("X-Auth-Email", email)
            .header("X-Auth-Password", password)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendRefreshTokenRequest(HttpClient client, String refreshToken)
      throws IOException, InterruptedException {
    HttpRequest request =
        itUtils.createRequest("/api/users/auth/refresh")
            .header("Content-Type", "application/json")
            .header("X-Refresh-Token", refreshToken)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendUserInfoRequest(HttpClient client, String accessToken)
      throws IOException, InterruptedException {

    HttpRequest request =
        itUtils.createRequest("/api/users/info")
            .header("X-Auth-Token", accessToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendUpdateUserInfoRequest(
      HttpClient client, String accessToken, String newName)
      throws IOException, InterruptedException {

    HttpRequest request =
        itUtils.createRequest("/api/users/update")
            .header("X-Auth-Token", accessToken)
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public HttpResponse<String> sendLogoutRequest(HttpClient client, String accessToken)
      throws IOException, InterruptedException {
    HttpRequest request =
        itUtils.createRequest("/api/users/auth/logout")
            .header("X-Auth-Token", accessToken)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public String extractAccessToken(HttpResponse<String> response) throws IOException {
    JsonNode root = itUtils.getRootNodeFromResponse(response);
    return root.get("AccessToken").asText();
  }
}

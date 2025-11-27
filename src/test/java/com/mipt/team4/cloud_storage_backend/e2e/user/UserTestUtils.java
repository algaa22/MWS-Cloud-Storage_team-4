package com.mipt.team4.cloud_storage_backend.e2e.user;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class UserTestUtils {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String BASE_URL = "http://localhost:8082/api/v1";

  public static HttpResponse<String> sendRegisterRequest(
      HttpClient client, String email, String password, String name)
      throws IOException, InterruptedException {

    String json = String.format(
        "{\"email\":\"%s\", \"password\":\"%s\", \"name\":\"%s\"}",
        email, password, name
    );

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/user/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendLoginRequest(
      HttpClient client, String email, String password)
      throws IOException, InterruptedException {

    String json = String.format(
        "{\"email\":\"%s\", \"password\":\"%s\"}",
        email, password
    );

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/user/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendRefreshTokenRequest(
      HttpClient client, String refreshToken)
      throws IOException, InterruptedException {

    String json = String.format("{\"refreshToken\":\"%s\"}", refreshToken);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/user/refresh"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendUserInfoRequest(HttpClient client, String accessToken)
      throws IOException, InterruptedException {

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/user"))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendUpdateUserInfoRequest(
      HttpClient client, String accessToken, String newName)
      throws IOException, InterruptedException {

    String json = String.format("{\"name\":\"%s\"}", newName);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/user"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static HttpResponse<String> sendLogoutRequest(
      HttpClient client, String refreshToken)
      throws IOException, InterruptedException {

    String json = String.format("{\"refreshToken\":\"%s\"}", refreshToken);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/user/logout"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static String extractAccessToken(HttpResponse<String> response) throws IOException {
    JsonNode root = TestUtils.getRootNodeFromResponse(response);
    return root.get("accessToken").asText();
  }

  public static String extractRefreshToken(HttpResponse<String> response) throws IOException {
    JsonNode root = TestUtils.getRootNodeFromResponse(response);
    return root.get("refreshToken").asText();
  }
}

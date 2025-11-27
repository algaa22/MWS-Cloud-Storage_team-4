package com.mipt.team4.cloud_storage_backend.e2e.user;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class UserAuthUtils {

  public record AuthResult(
      String email,
      String password,
      String accessToken,
      String refreshToken
  ) {}

  public static AuthResult sendRegisterRandomUser(HttpClient client) {
    try {
      String email = "test_" + UUID.randomUUID() + "@example.com";
      String password = "pass_" + UUID.randomUUID();

      String body = """
                    {
                        "email": "%s",
                        "password": "%s",
                        "userName": "AutoUser"
                    }
                    """.formatted(email, password);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create("http://localhost:8080/api/user/register"))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();

      HttpResponse<String> response =
          client.send(request, HttpResponse.BodyHandlers.ofString());

      JsonNode root = TestUtils.getRootNodeFromResponse(response);

      String accessToken = root.get("accessToken").asText();
      String refreshToken = root.get("refreshToken").asText();

      return new AuthResult(email, password, accessToken, refreshToken);

    } catch (Exception e) {
      throw new RuntimeException("Failed to register random user", e);
    }
  }
}

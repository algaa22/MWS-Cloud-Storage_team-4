package com.mipt.team4.cloud_storage_backend.e2e.user;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class UserAuthUtils {
  public static String sendRegisterTestUserRequest(
      HttpClient client, String email, String username, String password) {
    HttpRequest request =
        TestUtils.createRequest("/api/users/auth/register")
            .header("X-Auth-Email", email)
            .header("X-Auth-Username", username)
            .header("X-Auth-Password", password)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String responseBody = response.body();

      if (response.statusCode() != HttpStatus.SC_CREATED)
        throw new RuntimeException("Failed to register test user: " + responseBody);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(responseBody);

      return rootNode.get("token").asText();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}

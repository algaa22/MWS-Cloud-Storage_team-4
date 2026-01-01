package com.mipt.team4.cloud_storage_backend.e2e.user;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UserAuthUtils {

  static int usersCounter = 0;

  // TODO: перенести в usertestutils

  public static String sendRegisterRandomUserRequest(HttpClient client)
      throws IOException, InterruptedException {
    HttpResponse<String> response = sendRegisterTestUserRequest(client, createRandomUser());
    String responseBody = response.body();

    // TODO: RE?
    if (response.statusCode() != HttpStatus.SC_CREATED) {
      throw new RuntimeException("Failed to register test user: " + responseBody);
    }

    return UserITUtils.extractAccessToken(response);
  }

  public static TestUserDto createRandomUser() {
    return new TestUserDto(
        "deadlyparkourkillerdarkbrawlstarsassassinstalkersniper1998rus",
        usersCounter++ + "@email.com",
        "superpassword1488");
  }

  // TODO: нужен ли TestUserDto?
  public static HttpResponse<String> sendRegisterTestUserRequest(
      HttpClient client, TestUserDto user) throws IOException, InterruptedException {
    return sendRegisterTestUserRequest(client, user.email(), user.password(), user.userName());
  }

  public static HttpResponse<String> sendRegisterTestUserRequest(
      HttpClient client, String email, String password, String userName)
      throws IOException, InterruptedException {
    HttpRequest request =
        TestUtils.createRequest("/api/users/auth/register")
            .header("X-Auth-Email", email)
            .header("X-Auth-Username", userName)
            .header("X-Auth-Password", password)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}
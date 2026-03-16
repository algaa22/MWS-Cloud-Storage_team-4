package com.mipt.team4.cloud_storage_backend.e2e.user.utils;

import static org.junit.Assert.assertEquals;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.utils.ITUtils;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAuthUtils {
  private static int usersCounter = 0;
  private final UserITUtils userITUtils;
  private final ITUtils itUtils;

  // TODO: перенести в usertestutils

  public String sendRegisterRandomUserRequest(HttpClient client)
      throws IOException, InterruptedException {
    HttpResponse<String> response = sendRegisterTestUserRequest(client, createRandomUser());
    assertEquals(HttpStatus.SC_CREATED, response.statusCode());

    return userITUtils.extractAccessToken(response);
  }

  public TestUserDto createRandomUser() {
    return new TestUserDto(
        "deadlyparkourkillerdarkbrawlstarsassassinstalkersniper1998rus",
        usersCounter++ + "@email.com",
        "superPassword$1488");
  }

  // TODO: нужен ли TestUserDto?
  public HttpResponse<String> sendRegisterTestUserRequest(HttpClient client, TestUserDto user)
      throws IOException, InterruptedException {
    return sendRegisterTestUserRequest(client, user.email(), user.password(), user.userName());
  }

  public HttpResponse<String> sendRegisterTestUserRequest(
      HttpClient client, String email, String password, String userName)
      throws IOException, InterruptedException {
    HttpRequest request =
        itUtils
            .createRequest("/api/users/auth/register")
            .header("X-Auth-Email", email)
            .header("X-Auth-Username", userName)
            .header("X-Auth-Password", password)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }
}

package com.mipt.team4.cloud_storage_backend.e2e.user;

import static org.junit.jupiter.api.Assertions.*;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;

import java.io.IOException;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class UserSmokeTest extends BaseUserE2ETest {

  @Test
  public void shouldRegisterUser() throws IOException, InterruptedException {
    HttpResponse<String> response =
        UserTestUtils.sendRegisterRequest(client, TEST_EMAIL, TEST_PASSWORD, "TestUser");

    assertEquals(200, response.statusCode());

    JsonNode root = TestUtils.getRootNodeFromResponse(response);
    assertNotNull(root.get("accessToken"));
    assertNotNull(root.get("refreshToken"));
  }

  @Test
  public void shouldNotRegisterUserTwice() throws IOException, InterruptedException {
    UserTestUtils.sendRegisterRequest(client, TEST_EMAIL, TEST_PASSWORD, "User1");

    HttpResponse<String> response =
        UserTestUtils.sendRegisterRequest(client, TEST_EMAIL, TEST_PASSWORD, "User1");

    assertEquals(409, response.statusCode());
  }

  @Test
  public void shouldLoginUser() throws IOException, InterruptedException {
    UserTestUtils.sendRegisterRequest(client, TEST_EMAIL, TEST_PASSWORD, "UserLogin");

    HttpResponse<String> response =
        UserTestUtils.sendLoginRequest(client, TEST_EMAIL, TEST_PASSWORD);

    assertEquals(200, response.statusCode());

    JsonNode root = TestUtils.getRootNodeFromResponse(response);
    assertNotNull(root.get("accessToken"));
    assertNotNull(root.get("refreshToken"));
  }

  @Test
  public void shouldNotLoginWithWrongPassword() throws IOException, InterruptedException {
    UserTestUtils.sendRegisterRequest(client, TEST_EMAIL, TEST_PASSWORD, "UserLoginError");

    HttpResponse<String> response =
        UserTestUtils.sendLoginRequest(client, TEST_EMAIL, "wrong_password");

    assertEquals(401, response.statusCode());
  }

  @Test
  public void shouldRefreshToken() throws IOException, InterruptedException {
    HttpResponse<String> registerResponse =
        UserTestUtils.sendRegisterRequest(client, TEST_EMAIL, TEST_PASSWORD, "User1");

    String refreshToken = UserTestUtils.extractRefreshToken(registerResponse);

    HttpResponse<String> refreshResponse =
        UserTestUtils.sendRefreshTokenRequest(client, refreshToken);

    assertEquals(200, refreshResponse.statusCode());

    JsonNode root = TestUtils.getRootNodeFromResponse(refreshResponse);
    assertNotNull(root.get("accessToken"));
    assertNotNull(root.get("refreshToken"));
  }

  @Test
  public void shouldGetUserInfo() throws IOException, InterruptedException {
    HttpResponse<String> register =
        UserTestUtils.sendRegisterRequest(client, TEST_EMAIL, TEST_PASSWORD, "User1");

    String accessToken = UserTestUtils.extractAccessToken(register);

    HttpResponse<String> response =
        UserTestUtils.sendUserInfoRequest(client, accessToken);

    assertEquals(200, response.statusCode());
  }

  @Test
  public void shouldLogoutUser() throws IOException, InterruptedException {
    HttpResponse<String> register =
        UserTestUtils.sendRegisterRequest(client, TEST_EMAIL, TEST_PASSWORD, "User1");

    String refreshToken = UserTestUtils.extractRefreshToken(register);
    String accessToken = UserTestUtils.extractAccessToken(register);

    HttpResponse<String> logoutResponse =
        UserTestUtils.sendLogoutRequest(client, refreshToken);

    assertEquals(200, logoutResponse.statusCode());

    HttpResponse<String> infoResponse =
        UserTestUtils.sendUserInfoRequest(client, accessToken);

    assertEquals(401, infoResponse.statusCode());
  }
}

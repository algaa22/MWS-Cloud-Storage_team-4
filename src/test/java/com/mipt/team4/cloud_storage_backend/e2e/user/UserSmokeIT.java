package com.mipt.team4.cloud_storage_backend.e2e.user;

import static org.junit.jupiter.api.Assertions.*;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;

import java.io.IOException;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class UserSmokeIT extends BaseUserIT {

  @Test
  public void shouldRegisterUser() throws IOException, InterruptedException {
    HttpResponse<String> response =
        UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "TestUser");

    assertEquals(HttpStatus.SC_CREATED, response.statusCode());

    JsonNode root = TestUtils.getRootNodeFromResponse(response);
    assertNotNull(root.get("AccessToken"));
    assertNotNull(root.get("RefreshToken"));
  }

  @Test
  public void shouldNotRegisterUserTwice() throws IOException, InterruptedException {
    UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "User1");

    HttpResponse<String> response =
        UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "User1");

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
  }

  @Test
  public void shouldLoginUser() throws IOException, InterruptedException {
    UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "UserLogin");

    HttpResponse<String> response = UserITUtils.sendLoginRequest(client, testEmail, testPassword);

    assertEquals(HttpStatus.SC_OK, response.statusCode());

    JsonNode root = TestUtils.getRootNodeFromResponse(response);
    assertNotNull(root.get("AccessToken"));
    assertNotNull(root.get("RefreshToken"));
  }

  @Test
  public void shouldNotLoginWithWrongPassword() throws IOException, InterruptedException {
    UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "UserLoginError");

    HttpResponse<String> response =
        UserITUtils.sendLoginRequest(client, testEmail, "wrong_password");

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
  }

  @Test
  public void shouldRefreshToken() throws IOException, InterruptedException {
    HttpResponse<String> registerResponse =
        UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "RefreshUser");

    JsonNode registerRoot = TestUtils.getRootNodeFromResponse(registerResponse);
    String oldRefreshToken = registerRoot.get("RefreshToken").asText();

    HttpResponse<String> refreshResponse =
        UserITUtils.sendRefreshTokenRequest(client, oldRefreshToken);

    assertEquals(HttpStatus.SC_OK, refreshResponse.statusCode());

    JsonNode refreshRoot = TestUtils.getRootNodeFromResponse(refreshResponse);
    assertNotNull(refreshRoot.get("AccessToken"));
    assertNotNull(refreshRoot.get("RefreshToken"));

    String oldAccessToken = registerRoot.get("AccessToken").asText();
    String newAccessToken = refreshRoot.get("AccessToken").asText();
    String newRefreshToken = refreshRoot.get("RefreshToken").asText();

    assertNotEquals(oldAccessToken, newAccessToken);
    assertNotEquals(oldRefreshToken, newRefreshToken);
  }

  // TODO: update user info

  // TODO: проверка на ответ
  @Test
  public void shouldGetUserInfo() throws IOException, InterruptedException {
    HttpResponse<String> register =
        UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "User1");

    String accessToken = UserITUtils.extractAccessToken(register);

    HttpResponse<String> response = UserITUtils.sendUserInfoRequest(client, accessToken);

    assertEquals(HttpStatus.SC_OK, response.statusCode());
  }

  @Test
  public void shouldLogoutUser() throws IOException, InterruptedException {
    HttpResponse<String> register =
        UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "LogoutUser");

    String accessToken = UserITUtils.extractAccessToken(register);

    HttpResponse<String> logoutResponse = UserITUtils.sendLogoutRequest(client, accessToken);

    assertEquals(HttpStatus.SC_OK, logoutResponse.statusCode());

    HttpResponse<String> infoResponse = UserITUtils.sendUserInfoRequest(client, accessToken);
    assertEquals(HttpStatus.SC_BAD_REQUEST, infoResponse.statusCode());
  }

  @Test
  public void shouldNotRefreshWithInvalidToken() throws IOException, InterruptedException {
    HttpResponse<String> response =
        UserITUtils.sendRefreshTokenRequest(client, "invalid-refresh-token");

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
  }

  @Test
  public void shouldNotRefreshWithUsedToken() throws IOException, InterruptedException {
    HttpResponse<String> registerResponse =
        UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "UsedTokenUser");

    JsonNode registerRoot = TestUtils.getRootNodeFromResponse(registerResponse);
    String oldRefreshToken = registerRoot.get("RefreshToken").asText();

    HttpResponse<String> firstRefresh =
        UserITUtils.sendRefreshTokenRequest(client, oldRefreshToken);
    assertEquals(HttpStatus.SC_OK, firstRefresh.statusCode());

    HttpResponse<String> secondRefresh =
        UserITUtils.sendRefreshTokenRequest(client, oldRefreshToken);
    assertEquals(HttpStatus.SC_BAD_REQUEST, secondRefresh.statusCode());
  }
}

package com.mipt.team4.cloud_storage_backend.e2e.user.smoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.e2e.user.BaseUserIT;
import com.mipt.team4.cloud_storage_backend.e2e.user.utils.UserAuthUtils;
import com.mipt.team4.cloud_storage_backend.e2e.user.utils.UserITUtils;
import com.mipt.team4.cloud_storage_backend.utils.ITUtils;
import java.io.IOException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

@Tag("smoke")
public class UserSmokeIT extends BaseUserIT {
  @Autowired private UserAuthUtils userAuthUtils;
  @Autowired private UserITUtils userITUtils;
  @Autowired private ITUtils itUtils;

  @Test
  public void shouldRegisterUser() throws IOException, InterruptedException {
    HttpResponse<String> response =
        userAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "TestUser");

    assertEquals(HttpStatus.SC_CREATED, response.statusCode());

    JsonNode root = itUtils.getRootNodeFromResponse(response);
    assertNotNull(root.get("AccessToken"));
    assertNotNull(root.get("RefreshToken"));
  }

  @Test
  public void shouldNotRegisterUserTwice() throws IOException, InterruptedException {
    userAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "User1");

    HttpResponse<String> response =
        userAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "User1");

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
  }

  @Test
  public void shouldLoginUser() throws IOException, InterruptedException {
    userAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "UserLogin");

    HttpResponse<String> response = userITUtils.sendLoginRequest(client, testEmail, testPassword);

    assertEquals(HttpStatus.SC_OK, response.statusCode());

    JsonNode root = itUtils.getRootNodeFromResponse(response);
    assertNotNull(root.get("AccessToken"));
    assertNotNull(root.get("RefreshToken"));
  }

  @Test
  public void shouldNotLoginWithWrongPassword() throws IOException, InterruptedException {
    userAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "UserLoginError");

    HttpResponse<String> response =
        userITUtils.sendLoginRequest(client, testEmail, "wrong_password");

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
  }

  @Test
  public void shouldRefreshToken() throws IOException, InterruptedException {
    HttpResponse<String> registerResponse =
        userAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "RefreshUser");

    JsonNode registerRoot = itUtils.getRootNodeFromResponse(registerResponse);
    String oldRefreshToken = registerRoot.get("RefreshToken").asText();

    HttpResponse<String> refreshResponse =
        userITUtils.sendRefreshTokenRequest(client, oldRefreshToken);

    assertEquals(HttpStatus.SC_OK, refreshResponse.statusCode());

    JsonNode refreshRoot = itUtils.getRootNodeFromResponse(refreshResponse);
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
        userAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "User1");

    String accessToken = userITUtils.extractAccessToken(register);

    HttpResponse<String> response = userITUtils.sendUserInfoRequest(client, accessToken);

    assertEquals(HttpStatus.SC_OK, response.statusCode());
  }

  @Test
  public void shouldLogoutUser() throws IOException, InterruptedException {
    HttpResponse<String> register =
        userAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "LogoutUser");

    String accessToken = userITUtils.extractAccessToken(register);

    HttpResponse<String> logoutResponse = userITUtils.sendLogoutRequest(client, accessToken);

    assertEquals(HttpStatus.SC_OK, logoutResponse.statusCode());

    HttpResponse<String> infoResponse = userITUtils.sendUserInfoRequest(client, accessToken);
    assertEquals(HttpStatus.SC_BAD_REQUEST, infoResponse.statusCode());
  }

  @Test
  public void shouldNotRefreshWithInvalidToken() throws IOException, InterruptedException {
    HttpResponse<String> response =
        userITUtils.sendRefreshTokenRequest(client, "invalid-refresh-token");

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
  }

  @Test
  public void shouldNotRefreshWithUsedToken() throws IOException, InterruptedException {
    HttpResponse<String> registerResponse =
        userAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "UsedTokenUser");

    JsonNode registerRoot = itUtils.getRootNodeFromResponse(registerResponse);
    String oldRefreshToken = registerRoot.get("RefreshToken").asText();

    HttpResponse<String> firstRefresh =
        userITUtils.sendRefreshTokenRequest(client, oldRefreshToken);
    assertEquals(HttpStatus.SC_OK, firstRefresh.statusCode());

    HttpResponse<String> secondRefresh =
        userITUtils.sendRefreshTokenRequest(client, oldRefreshToken);
    assertEquals(HttpStatus.SC_BAD_REQUEST, secondRefresh.statusCode());
  }
}

package com.mipt.team4.cloud_storage_backend.e2e.user;

import static org.junit.jupiter.api.Assertions.*;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.mipt.team4.cloud_storage_backend.utils.TestUtils;

import java.io.IOException;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;

public class UserSmokeIT extends BaseUserIT {
  @Test
  public void shouldRegisterUser() throws IOException, InterruptedException {
    HttpResponse<String> response =
        UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "TestUser");

    assertEquals(HttpStatus.SC_CREATED, response.statusCode());

    JsonNode root = TestUtils.getRootNodeFromResponse(response);
    assertNotNull(root.get("token"));
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
    assertNotNull(root.get("token"));
  }

  @Test
  public void shouldNotLoginWithWrongPassword() throws IOException, InterruptedException {
    UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "UserLoginError");

    HttpResponse<String> response =
        UserITUtils.sendLoginRequest(client, testEmail, "wrong_password");

    assertEquals(HttpStatus.SC_BAD_REQUEST, response.statusCode());
  }

  @Disabled
  @Test
  public void shouldRefreshToken() throws IOException, InterruptedException {
    // TODO: имитировать, что токен истек и проверять, что новый токен совпадает со старым
    //    HttpResponse<String> registerResponse =
    //        UserTestUtils.sendRegisterRequest(client, testEmail, testPassword, "User1");
    //
    //    String refreshToken = UserTestUtils.extractRefreshToken(registerResponse);
    //
    //    HttpResponse<String> refreshResponse =
    //        UserTestUtils.sendRefreshTokenRequest(client, refreshToken);
    //
    //    assertEquals(HttpStatus.SC_OK, refreshResponse.statusCode());
    //
    //    JsonNode root = TestUtils.getRootNodeFromResponse(refreshResponse);
    //    assertNotNull(root.get("accessToken"));
    //    assertNotNull(root.get("refreshToken"));
  }

  @Test
  public void shouldGetUserInfo() throws IOException, InterruptedException {
    // TODO: сделать так шобы он не падл ;)
    HttpResponse<String> register =
        UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "User1");

    String accessToken = UserITUtils.extractAccessToken(register);

    HttpResponse<String> response = UserITUtils.sendUserInfoRequest(client, accessToken);

    assertEquals(HttpStatus.SC_OK, response.statusCode());
  }

  @Test
  public void shouldLogoutUser() throws IOException, InterruptedException {
    HttpResponse<String> register =
        UserAuthUtils.sendRegisterTestUserRequest(client, testEmail, testPassword, "User1");

    String accessToken = UserITUtils.extractAccessToken(register);

    HttpResponse<String> logoutResponse = UserITUtils.sendLogoutRequest(client, accessToken);

    assertEquals(HttpStatus.SC_OK, logoutResponse.statusCode());

    HttpResponse<String> infoResponse = UserITUtils.sendUserInfoRequest(client, accessToken);

    assertEquals(HttpStatus.SC_BAD_REQUEST, infoResponse.statusCode());
  }
}

package com.mipt.team4.cloud_storage_backend.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LoginRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LogoutRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RegisterRequestDto;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import java.nio.charset.StandardCharsets;

public class UserController {
  private final ObjectMapper mapper = new ObjectMapper();
  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  public void registerUser(RegisterRequestDto registerRequest) throws ValidationFailedException {
    registerRequest.validate();
    service.registerUser(registerRequest);
  }

  public void loginUser(LoginRequestDto loginRequest) throws ValidationFailedException {
    loginRequest.validate();
    service.loginUser(loginRequest);
  }

  public void logoutUser(LogoutRequestDto logoutRequest) throws ValidationFailedException {
    logoutRequest.validate();
    service.logoutUser(logoutRequest);
  }
}

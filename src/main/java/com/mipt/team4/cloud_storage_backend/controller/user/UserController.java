package com.mipt.team4.cloud_storage_backend.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LoginRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LogoutRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RegisterRequestDto;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;

public class UserController {
  private final ObjectMapper mapper = new ObjectMapper();
  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  public String registerUser(RegisterRequestDto request)
      throws ValidationFailedException, UserAlreadyExistsException {
    request.validate();
    return service.registerUser(request);
  }

  public String loginUser(LoginRequestDto request)
      throws ValidationFailedException, InvalidEmailOrPassword, WrongPasswordException {
    request.validate();
    return service.loginUser(request);
  }

  public void logoutUser(LogoutRequestDto request)
      throws ValidationFailedException, UserNotFoundException, InvalidSessionException {
    request.validate();
    service.logoutUser(request);
  }

  public String refresh(RefreshTokenDto request) throws InvalidSessionException {
    // TODO
    if (request == null || request.refreshToken() == null) {
      throw new InvalidSessionException("refresh token required");
    }
    return service.refreshTokens(request.refreshToken());
  }
}

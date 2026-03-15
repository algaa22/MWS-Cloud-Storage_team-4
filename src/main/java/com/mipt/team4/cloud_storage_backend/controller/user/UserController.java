package com.mipt.team4.cloud_storage_backend.controller.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.TokenPairDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.LoginRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.RefreshTokenRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.RegisterRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SimpleUserRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdateUserInfoRequest;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserController {

  private final UserService service;
  private final AccessTokenService accessTokenService;

  public TokenPairDto registerUser(RegisterRequest request) {
    request.validate();
    return service.registerUser(request);
  }

  public TokenPairDto loginUser(LoginRequest request) {
    request.validate();
    return service.loginUser(request);
  }

  public void logoutUser(SimpleUserRequest request) {
    request.validate(accessTokenService);
    service.logoutUser(request);
  }

  public TokenPairDto refresh(RefreshTokenRequest request) {
    request.validate();
    return service.refreshTokens(request);
  }

  public UserDto getUserInfo(SimpleUserRequest request) {
    request.validate(accessTokenService);
    return service.getUserInfo(request);
  }

  public void updateUserInfo(UpdateUserInfoRequest request) {
    request.validate(accessTokenService);
    service.updateUserInfo(request);
  }
}

package com.mipt.team4.cloud_storage_backend.controller.user;

import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TokenPairDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.LoginRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.RefreshTokenRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.RegisterRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.SimpleUserRequest;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UpdateUserInfoRequest;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserController {

  private final UserService service;
  private final JwtService jwtService;

  public TokenPairDto registerUser(RegisterRequest request)
      throws ValidationFailedException, UserAlreadyExistsException {
    request.validate();
    return service.registerUser(request);
  }

  public TokenPairDto loginUser(LoginRequest request)
      throws ValidationFailedException, InvalidEmailOrPassword, WrongPasswordException {
    request.validate();
    return service.loginUser(request);
  }

  public void logoutUser(SimpleUserRequest request)
      throws ValidationFailedException, UserNotFoundException, InvalidSessionException {
    request.validate(jwtService);
    service.logoutUser(request);
  }

  public TokenPairDto refresh(RefreshTokenRequest request)
      throws InvalidSessionException, ValidationFailedException {
    request.validate();
    return service.refreshTokens(request);
  }

  public UserDto getUserInfo(SimpleUserRequest request)
      throws ValidationFailedException, UserNotFoundException {
    request.validate(jwtService);
    return service.getUserInfo(request);
  }

  public void updateUserInfo(UpdateUserInfoRequest request)
      throws ValidationFailedException, UserNotFoundException, WrongPasswordException {
    request.validate(jwtService);
    service.updateUserInfo(request);
  }
}

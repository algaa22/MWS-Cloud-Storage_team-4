package com.mipt.team4.cloud_storage_backend.controller.user;

import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.LoginRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RefreshTokenDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.RegisterRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.SimpleUserRequestDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.TokenPairDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UpdateUserInfoDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.service.user.UserService;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import org.springframework.stereotype.Controller;

@Controller
public class UserController {

  private final UserService service;
  private final JwtService jwtService;

  public UserController(UserService service, JwtService jwtService) {
    this.service = service;
    this.jwtService = jwtService;
  }

  public TokenPairDto registerUser(RegisterRequestDto request)
      throws ValidationFailedException, UserAlreadyExistsException {
    request.validate();
    return service.registerUser(request);
  }

  public TokenPairDto loginUser(LoginRequestDto request)
      throws ValidationFailedException, InvalidEmailOrPassword, WrongPasswordException {
    request.validate();
    return service.loginUser(request);
  }

  public void logoutUser(SimpleUserRequestDto request)
      throws ValidationFailedException, UserNotFoundException, InvalidSessionException {
    request.validate(jwtService);
    service.logoutUser(request);
  }

  public TokenPairDto refresh(RefreshTokenDto request)
      throws InvalidSessionException, ValidationFailedException {
    request.validate();
    return service.refreshTokens(request);
  }

  public UserDto getUserInfo(SimpleUserRequestDto request)
      throws ValidationFailedException, UserNotFoundException {
    request.validate(jwtService);
    return service.getUserInfo(request);
  }

  public void updateUserInfo(UpdateUserInfoDto request)
      throws ValidationFailedException, UserNotFoundException, WrongPasswordException {
    request.validate(jwtService);
    service.updateUserInfo(request);
  }
}

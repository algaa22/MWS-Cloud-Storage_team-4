package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.*;

import java.util.List;
import java.util.UUID;

public class UserService {
  public void registerUser(RegisterRequestDto registerRequest) {
    // TODO
  }

  public void loginUser(LoginRequestDto loginRequest) {
    // TODO
  }

  public void logoutUser(LogoutRequestDto logoutRequest) {
    // TODO
  }

  public UserDto getUser(UUID id) {
    return null;
  }

  public List<UserDto> getAllUsers() {
    return null;
  }

  public void deleteUser(UUID id) {}

  // UserResponseDto updateUser(UUID filePath, UserResponseDto dto);
}

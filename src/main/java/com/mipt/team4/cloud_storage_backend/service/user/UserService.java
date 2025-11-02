package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.model.user.dto.UserCreateDto;
import java.util.List;
import java.util.UUID;

public interface UserService {

  UserCreateDto createUser(UserCreateDto dto);

  UserDto getUser(UUID id);

  List<UserDto> getAllUsers();

  void deleteUser(UUID id);

  // UserResponseDto updateUser(UUID id, UserResponseDto dto);
}

package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import java.util.List;
import java.util.UUID;

public interface UserService {
  // TODO:
  //  странно, что UserDto, созданный, наверное, для получение данных о пользователе,
  //  передаётся в createUser на создание пользователя, и возвращается UserResponseDto
  //  Наверное лучше передавать сюда UserDto и возвращать UserEntity
  UserDto createUser(UserDto dto);

  UserDto getUser(UUID id);

  List<UserDto> getAllUsers();

  void deleteUser(UUID id);

  // UserResponseDto updateUser(UUID id, UserResponseDto dto);
}

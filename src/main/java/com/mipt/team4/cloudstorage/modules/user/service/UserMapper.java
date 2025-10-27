package com.mipt.team4.cloudstorage.modules.user.service;

import com.mipt.team4.cloudstorage.modules.user.dto.UserCreateDto;
import com.mipt.team4.cloudstorage.modules.user.dto.UserDto;
import com.mipt.team4.cloudstorage.modules.user.entity.UserEntity;
import java.util.UUID;

public class UserMapper {

  public static UserEntity toEntity(UserCreateDto dto) {
    return new UserEntity(
        UUID.randomUUID(),
        dto.name(),
        dto.email(),
        dto.password(),
        dto.phoneNumber(),
        dto.surname(),
        null);
  }

  public static UserDto toDto(UserEntity entity) {
    return new UserDto(
        entity.getName(),
        entity.getEmail(),
        entity.getSurname(),
        entity.getPhoneNumber(),
        entity.getFreeSpace());
  }
}

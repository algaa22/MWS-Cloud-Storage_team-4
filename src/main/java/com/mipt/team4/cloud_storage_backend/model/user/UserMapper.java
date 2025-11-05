package com.mipt.team4.cloud_storage_backend.model.user;

import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import java.util.UUID;

public class UserMapper {

  public static UserEntity toEntity(UserDto dto) {
    return new UserEntity(
        UUID.randomUUID(),
        dto.name(),
        dto.email(),
        dto.password(),
        dto.phoneNumber(),
        dto.storageLimit(),
        dto.used_storage(),
        dto.createdAt(),
        dto.isActive());
  }

  public static UserDto toDto(UserEntity entity) {
    return new UserDto(
        entity.getId(),
        entity.getName(),
        entity.getEmail(),
        entity.getPassword(),
        entity.getPhoneNumber(),
        entity.getStorageLimit(),
        entity.getUsedStorage(),
        entity.getCreatedAt(),
        entity.isActive());
  }
}

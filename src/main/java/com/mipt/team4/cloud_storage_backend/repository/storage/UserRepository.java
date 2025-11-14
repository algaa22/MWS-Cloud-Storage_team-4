package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.user.dto.UserDto;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;

import java.util.Optional;
import java.util.UUID;

public class UserRepository {
  public Optional<UserEntity> findByEmail(String email) {
    return null;
  }

  public void saveUser(UserEntity entity) {

  }

  public Optional<UserEntity> findById(UUID id) {
    return null;
  }

  public void updateUser(UserEntity user) {

  }
}

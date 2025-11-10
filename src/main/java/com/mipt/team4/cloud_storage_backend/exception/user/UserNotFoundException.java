package com.mipt.team4.cloud_storage_backend.exception.user;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
  public UserNotFoundException(UUID id) {
      super("Could not find user with id: " + id);
  }
}

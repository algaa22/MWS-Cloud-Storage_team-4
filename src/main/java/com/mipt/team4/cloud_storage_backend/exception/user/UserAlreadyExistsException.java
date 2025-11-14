package com.mipt.team4.cloud_storage_backend.exception.user;

import java.util.UUID;

public class UserAlreadyExistsException extends Exception {
  public UserAlreadyExistsException(UUID id) {
    super("User with ID " + id + " already exists");
  }

  public UserAlreadyExistsException(String email) {
    super("User with email " + email + " already exists");
  }
}

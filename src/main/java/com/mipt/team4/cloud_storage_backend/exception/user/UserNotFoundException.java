package com.mipt.team4.cloud_storage_backend.exception.user;

import java.util.UUID;

public class UserNotFoundException extends Exception {
  public UserNotFoundException(String token) {
    super("User with token " + token + " not found");
  }
}

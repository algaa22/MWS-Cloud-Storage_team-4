package com.mipt.team4.cloud_storage_backend.exception.user;

public class UserNotFoundException extends Exception {

  public UserNotFoundException(String token) {
    super("User with token " + token + " not found");
  }
}

package com.mipt.team4.cloud_storage_backend.exception.user;

public class UserNotFoundException extends Exception {
  public UserNotFoundException(String email) {
    super("User with email " + email + " not found");
  }
}

package com.mipt.team4.cloud_storage_backend.exception.user;

public class UserAlreadyExistsException extends RuntimeException {
  public UserAlreadyExistsException(String message, String email) {
    super("User with email " + email + " already exists" );
  }
}

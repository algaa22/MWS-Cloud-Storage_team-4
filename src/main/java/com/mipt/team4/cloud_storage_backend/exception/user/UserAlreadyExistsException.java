package com.mipt.team4.cloud_storage_backend.exception.user;

public class UserAlreadyExistsException extends Exception {
  public UserAlreadyExistsException(String email) {
    super("User with email " + email + " already exists" );
  }
}

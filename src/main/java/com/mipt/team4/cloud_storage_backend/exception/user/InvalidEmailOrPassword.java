package com.mipt.team4.cloud_storage_backend.exception.user;

public class InvalidEmailOrPassword extends RuntimeException {
  public InvalidEmailOrPassword(String message) {
    super(message);
  }
}

package com.mipt.team4.cloud_storage_backend.exception.user;

public class WrongPasswordException extends RuntimeException {
  public WrongPasswordException(String message) {
    super(message);
  }
}

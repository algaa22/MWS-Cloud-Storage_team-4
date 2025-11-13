package com.mipt.team4.cloud_storage_backend.exception.user;

public class WrongPasswordException extends Exception {
  public WrongPasswordException() {
    super("Password incorrect");
  }
}

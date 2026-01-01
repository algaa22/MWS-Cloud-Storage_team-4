package com.mipt.team4.cloud_storage_backend.exception.user;

public class InvalidEmailOrPassword extends Exception {

  public InvalidEmailOrPassword() {
    super("No such user");
  }
}

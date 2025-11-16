package com.mipt.team4.cloud_storage_backend.exception.session;

public class InvalidSessionException extends Exception {
  public InvalidSessionException(String token) {
    super("No session with token " + token + " was found");
  }
}

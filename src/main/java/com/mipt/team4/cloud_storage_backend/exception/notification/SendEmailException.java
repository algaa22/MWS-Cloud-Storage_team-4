package com.mipt.team4.cloud_storage_backend.exception.notification;

public class SendEmailException extends RuntimeException {

  public SendEmailException(String message) {
    super(message);
  }

  public SendEmailException(String message, Throwable cause) {
    super(message, cause);
  }

  public SendEmailException(Throwable cause) {
    super("Failed to send email", cause);
  }
}
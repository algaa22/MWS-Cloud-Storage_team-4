package com.mipt.team4.cloud_storage_backend.exception.transfer;

public class TransferNotStartedYetException extends Exception {

  public TransferNotStartedYetException() {
    super("HttpContent received without active HttpRequest");
  }
}

package com.mipt.team4.cloud_storage_backend.exception.service;

import java.util.UUID;

public class TranferSessionNotFoundException extends Exception {
  public TranferSessionNotFoundException(UUID sessionId) {
    super("Session with ID " + sessionId + " not found");
  }

  public TranferSessionNotFoundException(String sessionId) {
    super("Session with ID " + sessionId + " not found");
  }
}

package com.mipt.team4.cloud_storage_backend.exception.database;

import java.sql.SQLException;

public class DbCheckConnectionException extends RuntimeException {
  public DbCheckConnectionException(SQLException cause) {
    super("Failed to check connection", cause);
  }
}

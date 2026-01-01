package com.mipt.team4.cloud_storage_backend.exception.database;

import java.sql.SQLException;

public class DbCloseConnectionException extends RuntimeException {

  public DbCloseConnectionException(SQLException cause) {
    super("Failed to close connection", cause);
  }
}

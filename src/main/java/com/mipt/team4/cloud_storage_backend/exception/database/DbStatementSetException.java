package com.mipt.team4.cloud_storage_backend.exception.database;

import java.sql.SQLException;

public class DbStatementSetException extends RuntimeException {
  public DbStatementSetException(SQLException cause) {
    super("Failed to set parameters", cause);
  }
}

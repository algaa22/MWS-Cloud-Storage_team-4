package com.mipt.team4.cloud_storage_backend.exception.database;

import java.sql.SQLException;

public class DbExecuteUpdateException extends RuntimeException {
  public DbExecuteUpdateException(String query, SQLException cause) {
    super("Failed to execute update: " + query, cause);
  }
}

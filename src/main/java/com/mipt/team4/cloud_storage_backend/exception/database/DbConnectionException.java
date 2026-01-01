package com.mipt.team4.cloud_storage_backend.exception.database;

import java.sql.SQLException;

public class DbConnectionException extends RuntimeException {

  public DbConnectionException(SQLException cause) {
    super("Failed to connect to the database", cause);
  }
}

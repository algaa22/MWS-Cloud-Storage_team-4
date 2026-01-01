package com.mipt.team4.cloud_storage_backend.exception.database;

import java.sql.SQLException;

public class DbUnavailableException extends RuntimeException {

  public DbUnavailableException(SQLException cause) {
    super("Database connection lost", cause);
  }
}

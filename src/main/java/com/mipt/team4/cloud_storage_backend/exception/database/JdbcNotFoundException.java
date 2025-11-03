package com.mipt.team4.cloud_storage_backend.exception.database;

import java.sql.SQLException;

public class JdbcNotFoundException extends RuntimeException {
  public JdbcNotFoundException(ClassNotFoundException cause) {
    super("Postgres JDBC Driver not found", cause);
  }
}

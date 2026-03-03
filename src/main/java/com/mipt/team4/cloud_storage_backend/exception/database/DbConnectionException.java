package com.mipt.team4.cloud_storage_backend.exception.database;

import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import java.sql.SQLException;

public class DbConnectionException extends RecoverableStorageException {

  public DbConnectionException(SQLException cause) {
    super("Failed to connect to the database", cause);
  }
}

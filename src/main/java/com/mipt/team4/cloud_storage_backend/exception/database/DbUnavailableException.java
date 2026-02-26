package com.mipt.team4.cloud_storage_backend.exception.database;

import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import java.sql.SQLException;

public class DbUnavailableException extends RecoverableStorageException {

  public DbUnavailableException(SQLException cause) {
    super("Database connection lost", cause);
  }
}

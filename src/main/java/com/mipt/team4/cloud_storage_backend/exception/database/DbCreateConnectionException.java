package com.mipt.team4.cloud_storage_backend.exception.database;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import java.sql.SQLException;

public class DbCreateConnectionException extends FatalStorageException {

  public DbCreateConnectionException(SQLException cause) {
    super("Failed to connect to the database", cause);
  }
}

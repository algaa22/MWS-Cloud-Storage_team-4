package com.mipt.team4.cloud_storage_backend.exception.database;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import java.sql.SQLException;

public class DbCloseConnectionException extends FatalStorageException {

  public DbCloseConnectionException(SQLException cause) {
    super("Failed to close connection", cause);
  }
}

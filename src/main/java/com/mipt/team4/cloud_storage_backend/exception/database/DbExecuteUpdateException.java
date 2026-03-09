package com.mipt.team4.cloud_storage_backend.exception.database;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

import java.sql.SQLException;

public class DbExecuteUpdateException extends FatalStorageException {

    public DbExecuteUpdateException(String query, SQLException cause) {
        super("Failed to execute update: " + query, cause);
    }
}

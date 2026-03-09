package com.mipt.team4.cloud_storage_backend.exception.database;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

import java.sql.SQLException;

public class DbExecuteQueryException extends FatalStorageException {

    public DbExecuteQueryException(String query, SQLException cause) {
        super("Failed to execute query: " + query, cause);
    }
}

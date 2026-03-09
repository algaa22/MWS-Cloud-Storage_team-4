package com.mipt.team4.cloud_storage_backend.exception.database;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

import java.sql.SQLException;

public class DbCheckConnectionException extends FatalStorageException {

    public DbCheckConnectionException(SQLException cause) {
        super("Failed to check connection", cause);
    }
}

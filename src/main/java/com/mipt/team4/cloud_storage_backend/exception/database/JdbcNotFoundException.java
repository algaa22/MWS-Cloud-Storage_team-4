package com.mipt.team4.cloud_storage_backend.exception.database;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class JdbcNotFoundException extends FatalStorageException {

    public JdbcNotFoundException(ClassNotFoundException cause) {
        super("Postgres JDBC Driver not found", cause);
    }
}

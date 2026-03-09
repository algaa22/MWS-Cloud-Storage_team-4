package com.mipt.team4.cloud_storage_backend.exception.database;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class DbCreateTableException extends FatalStorageException {

    public DbCreateTableException(String table, Throwable cause) {
        super("Cannot create table \"" + table + "\"", cause);
    }
}

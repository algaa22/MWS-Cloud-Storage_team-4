package com.mipt.team4.cloud_storage_backend.exception.database;

public class DbCreateTableException extends RuntimeException {
  public DbCreateTableException(String table, Throwable cause) {
    super("Cannot create table \"" + table + "\"", cause);
  }
}

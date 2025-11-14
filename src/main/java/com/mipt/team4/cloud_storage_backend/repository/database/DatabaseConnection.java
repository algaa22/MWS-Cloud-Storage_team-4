package com.mipt.team4.cloud_storage_backend.repository.database;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import java.sql.SQLException;
import java.util.List;

public interface DatabaseConnection {
  void connect() throws SQLException;

  <T> List<T> executeQuery(
      String query, List<Object> params, PostgresConnection.ResultSetMapper<T> mapper);

  int executeUpdate(String query, List<Object> params);

  void disconnect() throws SQLException;
}

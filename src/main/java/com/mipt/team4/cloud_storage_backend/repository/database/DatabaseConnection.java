package com.mipt.team4.cloud_storage_backend.repository.database;

import java.sql.SQLException;
import java.util.List;

public interface DatabaseConnection {
  void connect() throws SQLException;

  <T> List<T> executeQuery(
      String query, List<Object> params, PostgresConnection.ResultSetMapper<T> mapper)
      throws SQLException;

  void disconnect() throws SQLException;
}

package com.mipt.team4.cloud_storage_backend.repository.database;

import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.exception.database.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostgresConnection implements DatabaseConnection {
  private final DatabaseConfig config;

  private Connection connection;

  public PostgresConnection(DatabaseConfig config) {
    this.config = config;
  }

  @Override
  public void connect() {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new JdbcNotFoundException(e);
    }

    if (isConnected()) return;

    try {
      connection = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
      createFilesTable();
      createUsersTable();
      // TODO: миграции
    } catch (SQLException e) {
      throw new DbConnectionException(e);
    }
  }

  public boolean isConnected() {
    try {
      return connection != null && !connection.isClosed();
    } catch (SQLException e) {
      throw new DbCheckConnectionException(e);
    }
  }

  @Override
  public <T> List<T> executeQuery(String query, List<Object> params, ResultSetMapper<T> mapper) throws DbExecuteQueryException {
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      setParameters(statement, params);

      ResultSet resultSet = statement.executeQuery();
      List<T> results = new ArrayList<>();

      while (resultSet.next()) {
        results.add(mapper.map(resultSet));
      }

      return results;
    } catch (SQLException e) {
      throw new DbExecuteQueryException(query, e);
    }
  }

  public int executeUpdate(String query, List<Object> params) throws DbExecuteUpdateException {
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      setParameters(statement, params);

      return statement.executeUpdate();
    } catch (SQLException e) {
      throw new DbExecuteUpdateException(query, e);
    }
  }

  private void setParameters(PreparedStatement statement, List<Object> params) throws SQLException {
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        statement.setObject(i + 1, params.get(i));
      }
    }
  }

    private void createFilesTable() {
        String createFilesSql = """
            CREATE TABLE IF NOT EXISTS files (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                storage_path VARCHAR(500) NOT NULL,
                file_size BIGINT NOT NULL,
                mime_type VARCHAR(100),
                tags VARCHAR(500),
                visibility VARCHAR(20) DEFAULT 'private',
                is_deleted BOOLEAN DEFAULT false
            )
        """;

        try {
            executeUpdate(createFilesSql, List.of());
        } catch (DbExecuteUpdateException e) {
            throw new DbCreateTableException("files", e);
        }
    }

    private void createUsersTable() {
        String createUsersSql = """
            CREATE TABLE IF NOT EXISTS users (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                email VARCHAR(255) UNIQUE NOT NULL,
                phone_number VARCHAR(100) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                username VARCHAR(100) NOT NULL,
                storage_limit BIGINT DEFAULT 10737418240,
                used_storage BIGINT DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_active BOOLEAN DEFAULT true
            )
        """;

        try {
            executeUpdate(createUsersSql, List.of());
        } catch (DbExecuteUpdateException e) {
            throw new DbCreateTableException("users", e);
        }

    }

  @Override
  public void disconnect() {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new DbCloseConnectionException(e);
    }
  }

  @FunctionalInterface
  public interface ResultSetMapper<T> {
    T map(ResultSet resultSet) throws SQLException;
  }
}

package com.mipt.team4.cloud_storage_backend.repository.database;

import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.exception.database.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostgresConnection implements DatabaseConnection {
  private Connection connection;

  public PostgresConnection() {}

  @Override
  public void connect() {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new JdbcNotFoundException(e);
    }

    try {
      // TODO: если уже коннектед?
      connection =
          DriverManager.getConnection(
              DatabaseConfig.INSTANCE.getUrl(),
              DatabaseConfig.INSTANCE.getUsername(),
              DatabaseConfig.INSTANCE.getPassword());
      // TODO: создание таблицы
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
  public <T> List<T> executeQuery(String query, List<Object> params, ResultSetMapper<T> mapper) {
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      setParameters(statement, params);

      ResultSet resultSet = statement.executeQuery();
      List<T> results = new ArrayList<>();

      while (resultSet.next()) {
        results.add(mapper.map(resultSet));
      }

      return results;
    } catch (SQLException e) {
      handleSqlException(e);

      throw new DbExecuteQueryException(query, e);
    }
  }

  public int executeUpdate(String query, List<Object> params) {
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      setParameters(statement, params);

      return statement.executeUpdate();
    } catch (SQLException e) {
      handleSqlException(e);

      throw new DbExecuteUpdateException(query, e);
    }
  }

  private void handleSqlException(SQLException e) {
    // TODO: обработать больше ошибок
    if (e.getSQLState().startsWith("08"))
      throw new DbUnavailableException(e);
  }

  private void setParameters(PreparedStatement statement, List<Object> params) throws SQLException {
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        statement.setObject(i + 1, params.get(i));
      }
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

package com.mipt.team4.cloud_storage_backend.repository.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostgresConnection implements DatabaseConnection {
  private Connection connection;

  @Override
  public void connect() {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Postgres JDBC Driver not found", e);
    }

    // TODO: Создать класс Config, в котором будут методы Config.getDbUrl() и т.п.

    String URL = "jdbc:postgresql://postgres:5432/cloud_storage_db";
    String username = "postgres";
    String password = "super_secret_password_123";

    try {
      connection = DriverManager.getConnection(URL, username, password);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to connect to the database", e);
    }
  }

  @Override
  public <T> List<T> executeQuery(String query,
                                  List<Object> params,
                                  ResultSetMapper<T> mapper) {
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      setParameters(statement, params);

      ResultSet resultSet = statement.executeQuery();
      List<T> results = new ArrayList<>();

      while (resultSet.next()) {
        results.add(mapper.map(resultSet));
      }

      return results;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to execute query: " + query, e);
    }
  }

  public int executeUpdate(String query, List<Object> params) {
    try (PreparedStatement statement = connection.prepareStatement(query)) {
      setParameters(statement, params);

      return statement.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to execute update: " + query, e);
    }
  }

  private void setParameters(PreparedStatement statement, List<Object> params) {
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        try {
          statement.setObject(i + 1, params.get(i));
        } catch (SQLException e) {
          throw new RuntimeException("Failed to set parameters", e);
        }
      }
    }
  }

  @Override
  public void disconnect() {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to close connection", e);
    }
  }

  @FunctionalInterface
  public interface ResultSetMapper<T> {
    T map(ResultSet resultSet) throws SQLException;
  }
}

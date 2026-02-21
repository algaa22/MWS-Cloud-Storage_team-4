package com.mipt.team4.cloud_storage_backend.repository.database;

import com.mipt.team4.cloud_storage_backend.config.props.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.exception.database.DbCheckConnectionException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbCloseConnectionException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbConnectionException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbCreateTableException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteQueryException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.exception.database.DbUnavailableException;
import com.mipt.team4.cloud_storage_backend.exception.database.JdbcNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgresConnection implements DatabaseConnection {
  private final DatabaseConfig databaseConfig;

  private Connection connection;

  @PostConstruct
  public void init() {
    connect();
  }

  @Override
  public void connect() {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new JdbcNotFoundException(e);
    }

    if (isConnected()) {
      return;
    }

    try {
      connection =
          DriverManager.getConnection(
              databaseConfig.url(), databaseConfig.username(), databaseConfig.password());
    } catch (SQLException e) {
      throw new DbConnectionException(e);
    }

    createTables();
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
    if (e.getSQLState().startsWith("08")) {
      throw new DbUnavailableException(e);
    }
  }

  private void setParameters(PreparedStatement statement, List<Object> params) throws SQLException {
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        statement.setObject(i + 1, params.get(i));
      }
    }
  }

  private void createTables() {
    createUsersTable();
    createFilesTable();
    createRefreshTokensTable();
  }

  private void createFilesTable() {
    String createFilesSql =
        """
                CREATE TABLE IF NOT EXISTS files (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    parent_id UUID REFERENCES files(id) ON DELETE CASCADE,
                    name VARCHAR(500) NOT NULL,
                    size BIGINT NOT NULL,
                    mime_type VARCHAR(100),
                    tags VARCHAR(500),
                    visibility VARCHAR(20) DEFAULT 'private',
                    is_deleted BOOLEAN DEFAULT false,
                    is_directory BOOLEAN DEFAULT false,

                    status VARCHAR(20) NOT NULL DEFAULT 'READY',
                    operation_type VARCHAR(30),
                    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    retry_count INT DEFAULT 0,
                    error_message TEXT,

                    CONSTRAINT check_no_self_reference CHECK (id != parent_id)
                )
            """;

    try {
      executeUpdate(createFilesSql, List.of());
    } catch (DbExecuteUpdateException e) {
      throw new DbCreateTableException("files", e);
    }
  }

  private void createUsersTable() {
    String createUsersSql =
        """
                CREATE TABLE IF NOT EXISTS users (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    email VARCHAR(255) UNIQUE NOT NULL,
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

  private void createRefreshTokensTable() {
    String request =
        """
                CREATE TABLE IF NOT EXISTS refresh_tokens (
                    id UUID PRIMARY KEY,
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    token TEXT NOT NULL UNIQUE,
                    expires_at TIMESTAMP NOT NULL,
                    revoked BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """;

    try {
      executeUpdate(request, List.of());
    } catch (DbExecuteUpdateException e) {
      throw new DbCreateTableException("refresh_tokens", e);
    }
  }

  @PreDestroy
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

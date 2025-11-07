package com.mipt.team4.cloud_storage_backend.repository.repository.database;

import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.SQLException;
import java.util.List;

public abstract class AbstractPostgresTest {
  protected static PostgreSQLContainer<?> postgresContainer;

  @BeforeAll
  protected static void beforeAll() {
    postgresContainer = new PostgreSQLContainer<>("postgres:18.0");
    postgresContainer.start();
  }

  @AfterAll
  protected static void afterAll() {
    postgresContainer.stop();
  }

  protected static PostgresConnection createConnection() {
    DatabaseConfig databaseConfig = new DatabaseConfig(
            postgresContainer.getJdbcUrl(),
            postgresContainer.getDatabaseName(),
            postgresContainer.getPassword()
    );

    PostgresConnection postgresConnection = new PostgresConnection(databaseConfig);
    postgresConnection.connect();

    // TODO: убрать
    createUsersTable(postgresConnection);
    createFilesTable(postgresConnection);

    return postgresConnection;
  }

  // TODO: убрать
  private static void createFilesTable(PostgresConnection postgresConnection) {
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
      postgresConnection.executeUpdate(createFilesSql, List.of());
    } catch (DbExecuteUpdateException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO: убрать
  private static void createUsersTable(PostgresConnection postgresConnection) {
    String createUsersSql = """
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
      postgresConnection.executeUpdate(createUsersSql, List.of());
    } catch (DbExecuteUpdateException e) {
      throw new RuntimeException(e);
    }
  }
}

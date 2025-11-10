package com.mipt.team4.cloud_storage_backend.repository.repository.database;

import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

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

    return postgresConnection;
  }
}

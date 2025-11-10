package com.mipt.team4.cloud_storage_backend.repository.database;

import com.mipt.team4.cloud_storage_backend.exception.database.DbExecuteUpdateException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

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
    PostgresConnection postgresConnection = new PostgresConnection();
    postgresConnection.connect();

    return postgresConnection;
  }
}

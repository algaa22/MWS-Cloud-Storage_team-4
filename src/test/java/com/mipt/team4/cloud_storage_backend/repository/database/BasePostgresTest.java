package com.mipt.team4.cloud_storage_backend.repository.database;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class BasePostgresTest {

  protected static PostgreSQLContainer<?> postgresContainer;

  // TODO: два раза создается контейнер (еще в E2E) - норм?

  @BeforeAll
  protected static void beforeAll() {
    postgresContainer = TestUtils.createPostgresContainer();
    postgresContainer.start();
  }

  @AfterAll
  protected static void afterAll() {
    postgresContainer.stop();
  }

  protected static PostgresConnection createConnection() {
    PostgresConnection postgresConnection =
        new PostgresConnection(
            postgresContainer.getJdbcUrl(),
            postgresContainer.getUsername(),
            postgresContainer.getPassword());
    postgresConnection.connect();

    return postgresConnection;
  }
}

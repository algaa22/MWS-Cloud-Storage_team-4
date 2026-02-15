package com.mipt.team4.cloud_storage_backend.repository.database;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class BasePostgresTest {

  protected static PostgreSQLContainer<?> postgresContainer;

  // TODO: два раза создается контейнер (еще в E2E) - норм?

  @BeforeAll
  protected static void beforeAll() {
    postgresContainer = TestUtils.createPostgresContainer();
    postgresContainer.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // TODO: дублирование configureProperties()
    registry.add("database.url", postgresContainer::getJdbcUrl);
    registry.add("database.username", postgresContainer::getUsername);
    registry.add("database.password", postgresContainer::getPassword);
  }

  @AfterAll
  protected static void afterAll() {
    postgresContainer.stop();
  }
}

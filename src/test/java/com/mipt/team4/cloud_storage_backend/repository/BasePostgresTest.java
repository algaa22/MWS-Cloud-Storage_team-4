package com.mipt.team4.cloud_storage_backend.repository;

import com.mipt.team4.cloud_storage_backend.utils.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class BasePostgresTest {
  protected static PostgreSQLContainer<?> postgresContainer;

  @BeforeAll
  protected static void beforeAll() {
    postgresContainer = TestUtils.createPostgresContainer();
    postgresContainer.start();
  }

  @AfterAll
  protected static void afterAll() {
    postgresContainer.stop();
  }
}

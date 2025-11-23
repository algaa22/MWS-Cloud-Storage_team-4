package com.mipt.team4.cloud_storage_backend.utils;

import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class TestUtils {
  public static PostgreSQLContainer<?> createPostgresContainer() {
    return new PostgreSQLContainer<>("postgres:18.0");
  }

  public static MinIOContainer createMinioContainer() {
    return new MinIOContainer("minio/minio:latest");
  }

  public static PostgresConnection createConnection(PostgreSQLContainer<?> postgresContainer) {
    PostgresConnection postgresConnection =
        new PostgresConnection(
            postgresContainer.getJdbcUrl(),
            postgresContainer.getUsername(),
            postgresContainer.getPassword());
    postgresConnection.connect();

    return postgresConnection;
  }
}

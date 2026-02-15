package com.mipt.team4.cloud_storage_backend.utils;

import com.mipt.team4.cloud_storage_backend.config.props.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class TestUtils {
  public static PostgreSQLContainer<?> createPostgresContainer() {
    return new PostgreSQLContainer<>("postgres:18.0")
        .withDatabaseName("test_db")
        .withUsername("test_user")
        .withPassword("test_pass");
  }

  public static MinIOContainer createMinioContainer() {
    return new MinIOContainer("minio/minio:latest")
        .withUserName("test_user")
        .withPassword("test_pass");
  }

  public static PostgresConnection createConnection(PostgreSQLContainer<?> postgresContainer) {
    DatabaseConfig databaseConfig =
        new DatabaseConfig(
            postgresContainer.getJdbcUrl(),
            postgresContainer.getUsername(),
            postgresContainer.getPassword());

    PostgresConnection postgresConnection = new PostgresConnection(databaseConfig);
    postgresConnection.connect();

    return postgresConnection;
  }
}

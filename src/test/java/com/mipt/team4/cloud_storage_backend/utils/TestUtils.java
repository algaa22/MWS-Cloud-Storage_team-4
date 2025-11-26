package com.mipt.team4.cloud_storage_backend.utils;

import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.config.MinioConfig;
import com.mipt.team4.cloud_storage_backend.config.NettyConfig;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.repository.database.PostgresConnection;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.fail;

public class TestUtils {
  public static PostgreSQLContainer<?> createPostgresContainer() {
    return new PostgreSQLContainer<>("postgres:18.0")
        .withDatabaseName(DatabaseConfig.INSTANCE.getName())
        .withUsername(DatabaseConfig.INSTANCE.getUsername())
        .withPassword(DatabaseConfig.INSTANCE.getPassword());
  }

  public static MinIOContainer createMinioContainer() {
    return new MinIOContainer("minio/minio:latest")
        .withUserName(MinioConfig.INSTANCE.getUsername())
        .withPassword(MinioConfig.INSTANCE.getPassword());
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

  public static HttpRequest.Builder createRequest(String endpoint) {
    return HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + NettyConfig.INSTANCE.getPort() + endpoint));
  }

  public static void failWithException(Exception exception) {
    fail("Exception thrown in test", exception);
  }
}
